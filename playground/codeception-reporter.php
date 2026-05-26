<?php

use Codeception\Event\FailEvent;
use Codeception\Event\SuiteEvent;
use Codeception\Event\TestEvent;
use Codeception\Events;
use Codeception\Extension;
use Codeception\Step;
use Codeception\Step\Comment;
use Codeception\Test\Gherkin;
use Codeception\Test\Interfaces\ScenarioDriven;
use Codeception\Test\Test;
use PHPUnit\Framework\SelfDescribing;
use PHPUnit\Framework\TestCase;
use SebastianBergmann\Comparator\ComparisonFailure;

class PhpStorm_Codeception_ReportPrinter extends Extension
{
    protected string $testStatus = Events::TEST_SUCCESS;
    protected array $failures = [];
    private bool $isSummaryTestCountPrinted = false;
    private string $startedTestName;
    private string $flowId;

    public function _initialize(): void
    {
        $this->_reconfigure(['settings' => ['silent' => true]]); // turn off printing for everything else
    }

    /**
     * We are listening for events
     *
     * @var array<string, string>
     */
    public static array $events = [
        Events::TEST_ERROR   => 'addError',
        Events::TEST_WARNING => 'addWarning',
        Events::TEST_FAIL    => 'addFailure',
        Events::TEST_INCOMPLETE  => 'addIncompleteTest',
        Events::TEST_SKIPPED  => 'addSkippedTest',
        Events::SUITE_BEFORE => 'startTestSuite',
        Events::SUITE_AFTER => 'endTestSuite',
        Events::TEST_START => 'startTest',
        Events::TEST_END => 'endTest',
    ];

    /**
     * An error occurred.
     *
     * @param FailEvent $event
     */
    public function addError(FailEvent $event): void
    {
        $this->addFail(Events::TEST_ERROR, $event);
    }

    /**
     * A warning occurred.
     *
     * @param FailEvent $event
     */
    public function addWarning(FailEvent $event): void
    {
        $this->addFail(Events::TEST_ERROR, $event);
    }

    /**
     * A failure occurred.
     *
     * @param FailEvent $event
     */
    public function addFailure(FailEvent $event): void
    {
        $parameters = [];
        $fail = $event->getFail();
        if ($fail instanceof PHPUnit\Framework\ExpectationFailedException) {
            $comparisonFailure = $fail->getComparisonFailure();

            if ($comparisonFailure instanceof ComparisonFailure) {
                $expectedString = $comparisonFailure->getExpectedAsString();

                if (empty($expectedString)) {
                    $expectedString = self::getPrimitiveValueAsString($comparisonFailure->getExpected());
                }

                $actualString = $comparisonFailure->getActualAsString();

                if (empty($actualString)) {
                    $actualString = self::getPrimitiveValueAsString($comparisonFailure->getActual());
                }

                if (!is_null($actualString) && !is_null($expectedString)) {
                    $parameters['type']     = 'comparisonFailure';
                    $parameters['actual']   = $actualString;
                    $parameters['expected'] = $expectedString;
                }
            }
        }

        $this->addFail(Events::TEST_ERROR, $event, $parameters);
    }

    /**
     * Incomplete test.
     *
     * @param FailEvent $event
     */
    public function addIncompleteTest(FailEvent $event): void
    {
        $this->addIgnoredTest($event);
    }

    /**
     * Skipped test.
     *
     * @param FailEvent $event
     */
    public function addSkippedTest(FailEvent $event): void
    {
        $testName = self::getTestAsString($event->getTest());
        if ($this->startedTestName != $testName) {
            $this->startTest($event);
            $this->printEvent(
                'testIgnored',
                [
                    'name'    => $testName,
                    'message' => self::getMessage($event->getFail()),
                    'details' => self::getDetails($event->getFail()),
                ]
            );
            $this->endTest($event);
        } else {
            $this->addIgnoredTest($event);
        }
    }

    public function addIgnoredTest(FailEvent $event): void {
        $this->addFail(Events::TEST_SKIPPED, $event);
    }

    private function addFail($status, FailEvent $event, $parameters = []) {
        $key = self::getTestSignature($event->getTest());
        $this->testStatus = $status;
        $parameters['message'] = self::getMessage($event->getFail());
        $parameters['details'] = self::getDetails($event->getFail());

        $this->failures[$key][] = $parameters;
    }

    /**
     * A testsuite started.
     *
     * @param SuiteEvent $event
     */
    public function startTestSuite(SuiteEvent $event): void
    {
        if (stripos(ini_get('disable_functions'), 'getmypid') === false) {
            $this->flowId = getmypid();
        } else {
            $this->flowId = false;
        }

        if (!$this->isSummaryTestCountPrinted) {
            $this->isSummaryTestCountPrinted = true;

            $this->printEvent(
                'testCount',
                ['count' => $event->getSuite()->getTestCount()]
            );
        }

        $suiteName = $event->getSuite()->getName();

        if (empty($suiteName)) {
            return;
        }

        //TODO: configure 'locationHint' to navigate to 'unit', 'acceptance', 'functional' test suite
        //TODO: configure 'locationHint' to navigate to  DataProvider tests for Codeception earlier 2.2.6
        $parameters = ['name' => $suiteName];
        $this->printEvent('testSuiteStarted', $parameters);
    }

    /**
     * A testsuite ended.
     *
     * @param SuiteEvent $event
     */
    public function endTestSuite(SuiteEvent $event): void
    {
        $suiteName = $event->getSuite()->getName();

        if (empty($suiteName)) {
            return;
        }

        $parameters = ['name' => $suiteName];
        $this->printEvent('testSuiteFinished', $parameters);
    }

    public static function getTestSignature(SelfDescribing $testCase): string
    {
        if ($testCase instanceof Codeception\Test\Interfaces\Descriptive) {
            return $testCase->getSignature();
        }
        if ($testCase instanceof \PHPUnit\Framework\TestCase) {
            return get_class($testCase) . ':' . $testCase->getName(false);
        }
        return $testCase->toString();
    }

    public static function getTestAsString(SelfDescribing $testCase): string
    {
        if ($testCase instanceof Codeception\Test\Interfaces\Descriptive) {
            return $testCase->toString();
        }
        if ($testCase instanceof \PHPUnit\Framework\TestCase) {
            $text = $testCase->getName();
            $text = preg_replace('/([A-Z]+)([A-Z][a-z])/', '\\1 \\2', $text);
            $text = preg_replace('/([a-z\d])([A-Z])/', '\\1 \\2', $text);
            $text = preg_replace('/^test /', '', $text);
            $text = ucfirst(strtolower($text));
            $text = str_replace(['::', 'with data set'], [':', '|'], $text);
            return Codeception\Util\ReflectionHelper::getClassShortName($testCase) . ': ' . $text;
        }
        return $testCase->toString();
    }

    public static function getTestFileName(SelfDescribing $testCase): bool|string
    {
        if ($testCase instanceof Codeception\Test\Interfaces\Descriptive) {
            return $testCase->getFileName();
        }
        return (new ReflectionClass($testCase))->getFileName();
    }

    public static function getTestFullName(SelfDescribing $testCase): bool|string
    {
        if ($testCase instanceof Codeception\Test\Interfaces\Plain) {
            return self::getTestFileName($testCase);
        }
        if ($testCase instanceof Codeception\Test\Interfaces\Descriptive) {
            $signature = $testCase->getSignature(); // cut everything before ":" from signature
            return self::getTestFileName($testCase) . '::' . preg_replace('~^(.*?):~', '', $signature);
        }
        if ($testCase instanceof \PHPUnit\Framework\TestCase) {
            return self::getTestFileName($testCase) . '::' . $testCase->getName(false);
        }
        return self::getTestFileName($testCase) . '::' . $testCase->toString();
    }

    /**
     * A test started.
     *
     * @param TestEvent $event
     */
    public function startTest(TestEvent $event): void
    {
        $testName              = self::getTestAsString($event->getTest());
        $this->startedTestName = $testName;
        $location              = self::getTestLocation($event->getTest());
        $gherkin = self::getGherkinTestLocation($event->getTest());
        if ($gherkin != null) {
            $location = $gherkin;
        }
        $params                = ['name' => $testName, 'locationHint' => $location];

        if ($event->getTest() instanceof ScenarioDriven) {
            $this->printEvent('testSuiteStarted', $params);
        }
        else {
            $this->printEvent('testStarted', $params);
        }
    }

    /**
     * A test ended.
     *
     * @param TestEvent $event
     */
    public function endTest(TestEvent $event): void
    {
        $result = null;
        switch ($this->testStatus) {
            case Events::TEST_ERROR:
            case Events::TEST_FAIL:
                $result = 'testFailed';
                break;
            case Events::TEST_SKIPPED:
                $result = 'testIgnored';
                break;
        }

        $name = self::getTestAsString($event->getTest());
        if ($this->startedTestName != $name) {
            $name = $this->startedTestName;
        }
        $gherkin = self::getGherkinTestLocation($event->getTest());
        $duration = (int)(round($event->getTime(), 2) * 1000);
        if ($event->getTest() instanceof ScenarioDriven) {
            $steps = $event->getTest()->getScenario()->getSteps();
            $len = sizeof($steps);
            $printed = 0;
            for ($i = 0; $i < $len; $i++) {
                $step = $steps[$i];
                if ($step->getAction() == null && $step->getMetaStep()) {
                    $step = $step->getMetaStep();
                }

                if ($step instanceof Comment) {
                    // TODO: render comments in grey color?
                    // comments are not shown because at the moment it's hard to distinguish them from real tests.
                    // e.g. comment steps show descriptions from *.feature tests.
                    continue;
                }
                $printed++;
                $testName = sprintf('%s %s %s',
                    ucfirst($step->getPrefix()),
                    $step->getHumanizedActionWithoutArguments(),
                    $step->getHumanizedArguments()
                );
                $stepLocation = self::getStepLocation($step) ?? self::getTestLocation($event->getTest());
                $location = $gherkin != null ? $gherkin : $stepLocation;
                $this->printEvent('testStarted',
                    [
                        'name' => $testName,
                        'locationHint' => $location
                    ]);

                $params = ['name' => $testName];
                if ($i == $len - 1) {
                    $this->endPhpUntTest($event->getTest());
                    $this->printError($event->getTest(), $result, $testName);
                    $params['duration'] = $duration;
                }
                $this->printEvent('testFinished', $params);
            }

            if ($printed == 0 && $result != null) {
                $this->printEvent('testStarted', ['name' => $name]);
                $this->endPhpUntTest($event->getTest());
                $this->printError($event->getTest(), $result, $name);
                $this->printEvent('testFinished', [
                    'name' => $name,
                    'duration' => $duration
                ]);
            }

            $this->printEvent('testSuiteFinished', ['name' => $name]);
        }
        else {
            $this->endPhpUntTest($event->getTest());
            $this->printError($event->getTest(), $result, self::getTestAsString($event->getTest()));

            $this->printEvent(
                'testFinished',
                [
                    'name' => self::getTestAsString($event->getTest()),
                    'duration' => $duration
                ]
            );
        }
    }

    public function endPhpUntTest(\PHPUnit\Framework\Test $test): void
    {
        if ($test instanceof TestCase && !$test->hasExpectationOnOutput()) {
            $this->output->write($test->getActualOutput());
        }
    }

    private function printError(Test $test, $result, $name) {
        if ($result != null) {
            $this->testStatus = Events::TEST_PARSED;
            $key = self::getTestSignature($test);
            if (isset($this->failures[$key])) {
                $failures = $this->failures[$key];
                //TODO: check if it's possible to have sizeof($params) > 1
                assert(sizeof($failures) == 1);

                $params = $failures[0];
                $params['name'] = $name;
                $this->printEvent($result, $params);
                unset($this->failures[$key]);
            }
        }
    }

    private function printEvent(string $eventName, array $params = [])
    {
        $this->write("\n##teamcity[$eventName");

        if ($this->flowId) {
            $params['flowId'] = $this->flowId;
        }

        foreach ($params as $key => $value) {
            $escapedValue = self::escapeValue($value);
            $this->write(" $key='$escapedValue'");
        }

        $this->write("]\n");
    }

    private static function getGherkinTestLocation(Test $test): ?string
    {
        if ($test instanceof Gherkin) {
            return "file://" . $test->getFeatureNode()->getFile() . ":" . $test->getScenarioNode()->getLine();
        }
        return null;
    }

    private static function getMessage(Throwable $e): string
    {
        $message = '';

        if (!$e instanceof PHPUnit\Framework\Exception) {
            if (strlen(get_class($e)) != 0) {
                $message = $message . get_class($e);
            }

            if (strlen($message) != 0 && strlen($e->getMessage()) != 0) {
                $message = $message . ' : ';
            }
        }

        return $message . $e->getMessage();
    }

    /**
     * @param Throwable $e
     *
     * @return string
     */
    private static function getDetails(Throwable $e): string
    {
        $stackTrace = PHPUnit\Util\Filter::getFilteredStacktrace($e);
        $previous   = $e->getPrevious();

        while ($previous) {
            if (\PHPUnit\Runner\Version::series() < 10) {
                $exceptionString = PHPUnit\Framework\TestFailure::exceptionToString($previous);
            } else {
                $exceptionString = PHPUnit\Util\ThrowableToStringMapper::map($previous);
            }
            $stackTrace .= "\nCaused by\n" .
                $exceptionString . "\n" .
                PHPUnit\Util\Filter::getFilteredStacktrace($previous);

            $previous = $previous->getPrevious();
        }

        return ' ' . str_replace("\n", "\n ", $stackTrace);
    }

    private static function getPrimitiveValueAsString(mixed $value): string
    {
        if (is_null($value)) {
            return 'null';
        } elseif (is_bool($value)) {
            return $value ? 'true' : 'false';
        } elseif (is_scalar($value)) {
            return print_r($value, true);
        }
        return "";
    }

    private static function escapeValue($text): string
    {
        $text = str_replace('|', '||', $text);
        $text = str_replace("'", "|'", $text);
        $text = str_replace("\n", '|n', $text);
        $text = str_replace("\r", '|r', $text);
        $text = str_replace(']', '|]', $text);
        return str_replace('[', '|[', $text);
    }

    private static function getStepLocation(Step $step): ?string
    {
        $filePath = $step->getFilePath();
        if ($filePath != null) {
            $location = $step->getLineNumber() != null ? $filePath . ':' . $step->getLineNumber() : $filePath;
            return "file://$location";
        }
        return null;
    }

    private static function getTestLocation(Test $test): string
    {
        return "php_qn://" . self::getTestFullName($test);
    }
}
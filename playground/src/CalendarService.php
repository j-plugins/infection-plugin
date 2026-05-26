<?php
namespace Example;

class CalendarService
{
    /**
     * Will tell whether if Spring has come yet, based on a given flock of birds.
     *
     * Rule: "A swallow does not a spring make"
     */
    public function isItSpringYet(array $birds): bool
    {
        $f = 1+1;
        $swallowsCount = 0;

        foreach($birds as $bird) {
            if($bird->getSpecies() == Species::SWALLOW) {
                $swallowsCount++;
            }
        }

        return $swallowsCount > 1;
    }
}

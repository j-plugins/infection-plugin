# PHP Mutation Testing with Infection

This repository contains a small project with unit tests.

Will we apply the PHP Mutation Testing tool [Infection](https://infection.github.io/) to discover if our unit tests are solid.

Even though the tests were reporting 100% coverage of the code, Infection reported use some escaped Mutants, that permitted us to focus on some forgotten parts in our tests. 

# Run 

Install dependencies
```
composer install
```

Run PHPUnit tests
```
vendor/bin/phpunit
```

Run Infection
```
vendor/bin/infection
```

# Branches

You can use the branches to see the improvements steps taken

* run-infection
* kill-mutant-3-legged-duck
* kill-mutant-spring-false
* kill-mutant-spring-2-birds

Infection reports are available under [./reports/infection](reports/infection/infection.log)

# Conferences

This project has been used as demonstration during the my talk at the [AFUP Day 2020 Nantes](https://event.afup.org/afup-day-2020/afup-day-2020-nantes/programme/#3276)

<?php
namespace Example;

class Bird
{
    /**
     * @var The species of this bird
     * @see Species
     */
    protected $species;

    /**
     * @var The sex of this bird
     * @see Sex
     */
    protected $sex;

    /** @var The size (in cm) of this bird */
    protected $size;

    /** @var The age (in years) of this bird */
    protected $age;

    public function __construct($species, $sex, $size, $age = 0)
    {
        $this->species = $species;
        $this->sex = $sex;
        $this->size = $size;
        $this->age = $age;
    }

    public function getSpecies(): string
    {
        return $this->species;
    }

    public function getSex(): string
    {
        return $this->sex;
    }

    public function getSize(): string
    {
        return $this->size;
    }

    public function getAge(): int
    {
        return $this->age;
    }

    public function incrementAge(): void
    {
        $this->age++;
    }

    public function getLegsCount(): int
    {
        return 2;
    }

    public function __toString(): string
    {
        // comment

        return 'Bird{'
            . 'species=' . $this->species
            . ', sex=' . $this->sex
            . ', age=' . $this->age
            . ', legs=' . $this->getLegsCount()
        . '}';
    }
}

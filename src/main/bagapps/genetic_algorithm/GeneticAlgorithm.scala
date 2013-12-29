package bagapps.genetic_algorithm

import scala.collection.immutable
import scala.collection.immutable.{TreeBag => Population}
import scala.annotation.tailrec
import scala.util.hashing.Hashing
import scala.collection.mutable

trait Individual {
  def fitness: Double

  def epochTag: Int
}

trait Individuals[I <: Individual] extends Ordering[I] {
  def crossover(individualA: I, individualB: I, epoch: Int): I

  def random(epoch: Int): I

  def mutated(individual: I, epoch: Int): I

}

object GeneticAlgorithm {

  def run[I <: Individual](individuals: Individuals[I], numberIterations: Int = 20, populationSize: Int = 20, elitism: Double = 0.5d) {

    implicit val m = immutable.BagBucketConfiguration.Sorted.ofVectors[I](individuals)

    @tailrec
    def runRec(population: Population[I], iteration: Int): Population[I] = {
      println(s"Iteration $iteration".padTo(15, ' '))
      println("  best fitness: " + population.head.fitness)
      println("  best individual: " + population.head)

      if (iteration <= numberIterations) {

        val numberElites = (elitism * populationSize).toInt
        val numberCrossover = (populationSize - numberElites) / 2

        val bestPopulation = population.take(numberElites)

        // Create a new Bag (Population) for the new generation
        var newGeneration = Population.empty[I]

        for (_ <- 1 to numberCrossover) {
          val individualA = selectRandom(population)
          val individualB = selectRandom(population)
          val individualC = individuals.crossover(individualA, individualB, iteration)
          newGeneration = newGeneration + individuals.mutated(individualC, iteration)
        }

        val newPopulation = bestPopulation union newGeneration


        runRec(newPopulation, iteration + 1)
      }
      else population
    }

    val bestPopulation = runRec(randomPopulation(individuals, Population.empty[I], populationSize), 1)
  }

  def randomPopulation[I <: Individual](individuals: Individuals[I], emptyPopulation: Population[I], populationSize: Int) = {
    var population = emptyPopulation
    for (_ <- 1 to populationSize)
      population = population + individuals.random(0)
    population
  }

  def selectRandom[I <: Individual](population: Population[I]) = {
    population.toList((Math.random() * population.size).toInt)
  }

}
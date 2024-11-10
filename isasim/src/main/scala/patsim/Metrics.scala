// File: Metrics.scala
package patsim

object Metrics {
  var totalInstructions: Int = 0
  var totalCycles: Int = 0
  var cacheHits: Int = 0
  var cacheMisses: Int = 0
  var branchMispredictions: Int = 0
  var compressInstructions: Int = 0
  var decompressInstructions: Int = 0
  var mulInstructions: Int = 0
  var muluInstructions: Int = 0

  def reset(): Unit = {
    totalInstructions = 0
    totalCycles = 0
    cacheHits = 0
    cacheMisses = 0
    branchMispredictions = 0
    compressInstructions = 0
    decompressInstructions = 0
    mulInstructions = 0
    muluInstructions = 0
  }

  def printMetrics(): Unit = {
    println("\n=== Performance Metrics ===")
    println(s"Total Cycles: $totalCycles")
    println(s"Total Instructions: $totalInstructions")
    if (totalInstructions > 0)
      println(f"CPI: ${totalCycles.toDouble / totalInstructions}%.2f")
    else
      println("CPI: N/A")
    println(s"Cache Hits: $cacheHits")
    println(s"Cache Misses: $cacheMisses")
    if (cacheHits + cacheMisses > 0)
      println(f"Cache Hit Rate: ${(cacheHits.toDouble / (cacheHits + cacheMisses)) * 100}%.2f%%")
    else
      println("Cache Hit Rate: N/A")
    println(s"Branch Mispredictions: $branchMispredictions")
    println(s"Compress Instructions Executed: $compressInstructions")
    println(s"Decompress Instructions Executed: $decompressInstructions")
    println(s"MUL Instructions Executed: $mulInstructions")
    println(s"MULU Instructions Executed: $muluInstructions")
    println("============================\n")
  }
}

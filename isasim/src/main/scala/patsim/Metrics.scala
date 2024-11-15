// File: Metrics.scala
package patsim

object Metrics {
  private val lock = new Object()
  
  // Basic metrics
  private var _totalInstructions: Int = 0
  private var _totalCycles: Int = 0
  private var _pipelineFlushes: Int = 0
  
  // Cache metrics
  private var _cacheHits: Int = 0
  private var _cacheMisses: Int = 0
  
  // Branch metrics
  private var _branchMispredictions: Int = 0
  private var _totalBranches: Int = 0
  
  // Compression metrics
  private var _compressInstructions: Int = 0
  private var _decompressInstructions: Int = 0
  private var _compressionHits: Int = 0
  private var _compressionMisses: Int = 0
  private var _decompressionHits: Int = 0
  private var _decompressionMisses: Int = 0
  
  // Multiplication metrics
  private var _mulInstructions: Int = 0
  private var _muluInstructions: Int = 0
  
  // Thread-safe accessors
  def totalInstructions: Int = lock.synchronized { _totalInstructions }
  def totalCycles: Int = lock.synchronized { _totalCycles }
  def pipelineFlushes: Int = lock.synchronized { _pipelineFlushes }
  def cacheHits: Int = lock.synchronized { _cacheHits }
  def cacheMisses: Int = lock.synchronized { _cacheMisses }
  def branchMispredictions: Int = lock.synchronized { _branchMispredictions }
  def totalBranches: Int = lock.synchronized { _totalBranches }
  def compressInstructions: Int = lock.synchronized { _compressInstructions }
  def decompressInstructions: Int = lock.synchronized { _decompressInstructions }
  def compressionHits: Int = lock.synchronized { _compressionHits }
  def compressionMisses: Int = lock.synchronized { _compressionMisses }
  def decompressionHits: Int = lock.synchronized { _decompressionHits }
  def decompressionMisses: Int = lock.synchronized { _decompressionMisses }
  def mulInstructions: Int = lock.synchronized { _mulInstructions }
  def muluInstructions: Int = lock.synchronized { _muluInstructions }
  
  // Thread-safe increment methods
  def incrementInstructions(): Unit = lock.synchronized { _totalInstructions += 1 }
  def incrementCycles(): Unit = lock.synchronized { _totalCycles += 1 }
  def incrementPipelineFlushes(): Unit = lock.synchronized { _pipelineFlushes += 1 }
  def incrementCacheHits(): Unit = lock.synchronized { _cacheHits += 1 }
  def incrementCacheMisses(): Unit = lock.synchronized { _cacheMisses += 1 }
  def incrementBranchMispredictions(): Unit = lock.synchronized { _branchMispredictions += 1 }
  def incrementTotalBranches(): Unit = lock.synchronized { _totalBranches += 1 }
  def incrementCompressInstructions(): Unit = lock.synchronized { _compressInstructions += 1 }
  def incrementDecompressInstructions(): Unit = lock.synchronized { _decompressInstructions += 1 }
  def incrementCompressionHits(): Unit = lock.synchronized { _compressionHits += 1 }
  def incrementCompressionMisses(): Unit = lock.synchronized { _compressionMisses += 1 }
  def incrementDecompressionHits(): Unit = lock.synchronized { _decompressionHits += 1 }
  def incrementDecompressionMisses(): Unit = lock.synchronized { _decompressionMisses += 1 }
  def incrementMulInstructions(): Unit = lock.synchronized { _mulInstructions += 1 }
  def incrementMuluInstructions(): Unit = lock.synchronized { _muluInstructions += 1 }

  def reset(): Unit = lock.synchronized {
    _totalInstructions = 0
    _totalCycles = 0
    _pipelineFlushes = 0
    _cacheHits = 0
    _cacheMisses = 0
    _branchMispredictions = 0
    _totalBranches = 0
    _compressInstructions = 0
    _decompressInstructions = 0
    _compressionHits = 0
    _compressionMisses = 0
    _decompressionHits = 0
    _decompressionMisses = 0
    _mulInstructions = 0
    _muluInstructions = 0
  }

  def printMetrics(): Unit = {
    println("\n=== Performance Metrics ===")
    println(s"Total Cycles: $totalCycles")
    println(s"Total Instructions: $totalInstructions")
    println(s"Pipeline Flushes: $pipelineFlushes")
    
    if (totalInstructions > 0) {
      println(f"CPI: ${totalCycles.toDouble / totalInstructions}%.2f")
      println(f"IPC: ${totalInstructions.toDouble / totalCycles}%.2f")
    } else {
      println("CPI: N/A")
      println("IPC: N/A")
    }
    
    println("\n=== Cache Performance ===")
    println(s"Cache Hits: $cacheHits")
    println(s"Cache Misses: $cacheMisses")
    if (cacheHits + cacheMisses > 0) {
      println(f"Cache Hit Rate: ${(cacheHits.toDouble / (cacheHits + cacheMisses)) * 100}%.2f%%")
      println(f"Cache Miss Rate: ${(cacheMisses.toDouble / (cacheHits + cacheMisses)) * 100}%.2f%%")
    } else {
      println("Cache Hit/Miss Rate: N/A")
    }
    
    println("\n=== Branch Performance ===")
    println(s"Total Branches: $totalBranches")
    println(s"Branch Mispredictions: $branchMispredictions")
    if (totalBranches > 0) {
      println(f"Branch Misprediction Rate: ${(branchMispredictions.toDouble / totalBranches) * 100}%.2f%%")
    } else {
      println("Branch Misprediction Rate: N/A")
    }
    
    println("\n=== Compression Performance ===")
    println(s"Compress Instructions: $compressInstructions")
    println(s"Decompress Instructions: $decompressInstructions")
    println(s"Compression Hits: $compressionHits")
    println(s"Compression Misses: $compressionMisses")
    println(s"Decompression Hits: $decompressionHits")
    println(s"Decompression Misses: $decompressionMisses")
    if (compressionHits + compressionMisses > 0) {
      println(f"Compression Hit Rate: ${(compressionHits.toDouble / (compressionHits + compressionMisses)) * 100}%.2f%%")
    }
    if (decompressionHits + decompressionMisses > 0) {
      println(f"Decompression Hit Rate: ${(decompressionHits.toDouble / (decompressionHits + decompressionMisses)) * 100}%.2f%%")
    }
    
    println("\n=== Multiplication Performance ===")
    println(s"MUL Instructions: $mulInstructions")
    println(s"MULU Instructions: $muluInstructions")
    println(f"Multiplication Instruction Mix: ${((mulInstructions + muluInstructions).toDouble / totalInstructions) * 100}%.2f%%")
    
    println("============================\n")
  }
}

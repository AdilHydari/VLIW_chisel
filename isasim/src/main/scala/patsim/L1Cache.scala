// File: L1Cache.scala
package patsim

class L1Cache(cacheSize: Int = 16) {
  case class CacheLine(var tag: Int = -1, var data: Int = 0, var valid: Boolean = false, var dirty: Boolean = false)
  
  private val cache = Array.fill(cacheSize)(CacheLine())
  private val lock = new Object()
  
  // Additional Metrics
  private var readHits: Int = 0
  private var readMisses: Int = 0
  private var writeHits: Int = 0
  private var writeMisses: Int = 0
  private var writebacks: Int = 0

  def access(address: Int, isLoad: Boolean, writeData: Int = 0): (Boolean, Int) = lock.synchronized {
    val index = (address / 4) % cacheSize
    val tag = address / (4 * cacheSize)
    val line = cache(index)

    if (line.valid && line.tag == tag) {
      // Cache hit
      if (isLoad) {
        readHits += 1
        Metrics.incrementCacheHits()
        println(s"[L1Cache] Load Hit at index $index for address $address")
        (true, line.data)
      } else {
        writeHits += 1
        Metrics.incrementCacheHits()
        // Update cache line
        line.data = writeData
        line.dirty = true
        println(s"[L1Cache] Store Hit at index $index for address $address with data $writeData")
        (true, 0)
      }
    } else {
      // Cache miss
      if (isLoad) {
        readMisses += 1
        Metrics.incrementCacheMisses()
        
        // Handle writeback if necessary
        if (line.valid && line.dirty) {
          writebacks += 1
          println(s"[L1Cache] Writeback at index $index, old tag ${line.tag}")
        }
        
        // Load new line
        line.tag = tag
        line.data = simulateMemoryRead(address)
        line.valid = true
        line.dirty = false
        
        println(s"[L1Cache] Load Miss at index $index for address $address")
        (false, line.data)
      } else {
        writeMisses += 1
        Metrics.incrementCacheMisses()
        
        // Handle writeback if necessary
        if (line.valid && line.dirty) {
          writebacks += 1
          println(s"[L1Cache] Writeback at index $index, old tag ${line.tag}")
        }
        
        // Write new line
        line.tag = tag
        line.data = writeData
        line.valid = true
        line.dirty = true
        
        println(s"[L1Cache] Store Miss at index $index for address $address")
        (false, 0)
      }
    }
  }

  private def simulateMemoryRead(address: Int): Int = {
    // Simulate memory latency
    Thread.sleep(1)
    // Return simulated data (for now, just return 0)
    0
  }

  def flush(): Unit = lock.synchronized {
    for (i <- cache.indices) {
      if (cache(i).valid && cache(i).dirty) {
        writebacks += 1
        println(s"[L1Cache] Flush writeback at index $i, tag ${cache(i).tag}")
      }
      cache(i) = CacheLine()
    }
  }

  def printCacheMetrics(): Unit = {
    println("\n=== L1 Cache Detailed Metrics ===")
    println(s"Read Hits: $readHits")
    println(s"Read Misses: $readMisses")
    println(s"Write Hits: $writeHits")
    println(s"Write Misses: $writeMisses")
    println(s"Total Writebacks: $writebacks")
    
    val totalReads = readHits + readMisses
    val totalWrites = writeHits + writeMisses
    val totalAccesses = totalReads + totalWrites
    
    if (totalReads > 0) {
      println(f"Read Hit Rate: ${(readHits.toDouble / totalReads) * 100}%.2f%%")
    }
    if (totalWrites > 0) {
      println(f"Write Hit Rate: ${(writeHits.toDouble / totalWrites) * 100}%.2f%%")
    }
    if (totalAccesses > 0) {
      println(f"Overall Hit Rate: ${((readHits + writeHits).toDouble / totalAccesses) * 100}%.2f%%")
      println(f"Writeback Rate: ${(writebacks.toDouble / totalAccesses) * 100}%.2f%%")
    }
    println("==============================\n")
  }

  def reset(): Unit = lock.synchronized {
    for (i <- cache.indices) {
      cache(i) = CacheLine()
    }
    readHits = 0
    readMisses = 0
    writeHits = 0
    writeMisses = 0
    writebacks = 0
  }
}

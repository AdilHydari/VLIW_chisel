package patsim

class PipelineRegisters {
  private var current_de_ex: DE_EX = DE_EX()
  private var next_de_ex: DE_EX = DE_EX()
  private var current_ex_mem: EX_MEM = EX_MEM()
  private var next_ex_mem: EX_MEM = EX_MEM()
  private var current_mem_wb: MEM_WB = MEM_WB()
  private var next_mem_wb: MEM_WB = MEM_WB()
  private var current_compress: COMPRESS_STAGE = COMPRESS_STAGE()
  private var next_compress: COMPRESS_STAGE = COMPRESS_STAGE()

  def update(): Unit = synchronized {
    current_de_ex = next_de_ex
    current_ex_mem = next_ex_mem
    current_mem_wb = next_mem_wb
    current_compress = next_compress
    
    // Clear next stage registers
    next_de_ex = DE_EX()
    next_ex_mem = EX_MEM()
    next_mem_wb = MEM_WB()
    next_compress = COMPRESS_STAGE()
  }

  // Getters for current values
  def getDE_EX: DE_EX = current_de_ex
  def getEX_MEM: EX_MEM = current_ex_mem
  def getMEM_WB: MEM_WB = current_mem_wb
  def getCOMPRESS: COMPRESS_STAGE = current_compress

  // Setters for next values
  def setNextDE_EX(value: DE_EX): Unit = next_de_ex = value
  def setNextEX_MEM(value: EX_MEM): Unit = next_ex_mem = value
  def setNextMEM_WB(value: MEM_WB): Unit = next_mem_wb = value
  def setNextCOMPRESS(value: COMPRESS_STAGE): Unit = next_compress = value

  def flush(): Unit = synchronized {
    current_de_ex = DE_EX()
    current_ex_mem = EX_MEM()
    current_mem_wb = MEM_WB()
    current_compress = COMPRESS_STAGE()
    next_de_ex = DE_EX()
    next_ex_mem = EX_MEM()
    next_mem_wb = MEM_WB()
    next_compress = COMPRESS_STAGE()
  }
}
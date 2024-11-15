package patsim

class BranchUnit {
  private var branchTarget: Option[Int] = None
  private var branchTaken: Boolean = false
  private var lastPrediction: Int = 0

  def predict(pc: Int, instr: DecodedInstruction): Int = {
    // Simple static prediction: predict not taken
    lastPrediction = pc + 2
    lastPrediction
  }

  def resolve(pc: Int, instr: DecodedInstruction, condition: Boolean): (Boolean, Int) = {
    val target = if (condition) pc + instr.imm12 else pc + 2
    val mispredicted = target != lastPrediction
    
    if (mispredicted) {
      Metrics.branchMispredictions += 1
      branchTarget = Some(target)
      branchTaken = condition
    }
    
    (mispredicted, target)
  }

  def clearPrediction(): Unit = {
    branchTarget = None
    branchTaken = false
    lastPrediction = 0
  }

  def hasMisprediction: Boolean = branchTarget.isDefined
  def getMispredictionTarget: Int = branchTarget.getOrElse(0)
}
package test.kafka.test.kafka.bpmn;

public class TaskClock {

	private Long startTime;
	private Long endTime;

	public TaskClock(Long startTime) {
		super();
		this.setStartTime(startTime);
		this.setEndTime(null);
	}

	public Long getEndTime() {
		return endTime;
	}
	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public boolean isDone() {
		return this.endTime != null;
	}

	public String toString() {
		return "<TimeClock start=" + this.getStartTime() + " end=" + this.getEndTime() + ">";
	}

}

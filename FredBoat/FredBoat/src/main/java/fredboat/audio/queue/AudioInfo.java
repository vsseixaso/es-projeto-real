package fredboat.audio.queue;

public class AudioInfo {
	
	private long startPos;
	private long endPos;
	private String title;
	
	
	public AudioInfo(long startPos, long endPos, String title) {
		super();
		this.startPos = startPos;
		this.endPos = endPos;
		this.title = title;
	}


	public long getStartPos() {
		return startPos;
	}


	public void setStartPos(long startPos) {
		this.startPos = startPos;
	}


	public long getEndPos() {
		return endPos;
	}


	public void setEndPos(long endPos) {
		this.endPos = endPos;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}
	
	

}
package Personal.jocltest;

public class TextHandler {
	
	private int sheetIndex;
	private int letterWidth, letterHeight;
	private String alphabet;
	private GraphicsCardInterface gfx;
	
	public TextHandler(GraphicsCardInterface gfx, int letterWidth, int letterHeight, String alphabet, String path) {
		this.gfx = gfx;
		this.letterWidth = letterWidth;
		this.letterHeight = letterHeight;
		this.alphabet = alphabet;
		sheetIndex = gfx.loadTexture(path);
	}
	
	public void renderText(int canvas, String text, int x, int y, float scale) {
		text = text.toLowerCase();
		int[] dims = gfx.getSheetDims(sheetIndex);
		for(int i = 0; i < text.length(); i++) {
			int index = 0;
			while (index < alphabet.length() && text.charAt(i) != alphabet.charAt(index)) {
				index++;
			}
			gfx.render(canvas, 
					sheetIndex, x + (int)(i * letterWidth * scale), 
					y, 
					(index * letterWidth)%dims[0], 
					((index * letterWidth)/dims[0])*letterHeight, 
					letterWidth, 
					letterHeight, 
					scale, 
					scale, 
					false, false, 0, 0, 0, 0, 0, 0, 1f);
		}
	}
	
	public void renderOffsetText(int canvas, String text, int x, int y, float scale, float xWaveOffset, float yWaveOffset, int xWaveAmp, int yWaveAmp, float xWavePeriod, float yWavePeriod) {
		text = text.toLowerCase();
		int[] dims = gfx.getSheetDims(sheetIndex);
		for(int i = 0; i < text.length(); i++) {
			int index = 0;
			while (index < alphabet.length() && text.charAt(i) != alphabet.charAt(index)) {
				index++;
			}
			gfx.render(canvas, 
					sheetIndex, 
					x + (int)((i * letterWidth * scale) + (xWaveAmp * Math.sin(xWavePeriod * ((i+1) + xWaveOffset)))), 
					y + (int) (yWaveAmp * Math.sin(yWavePeriod * ((i + 1) + yWaveOffset))), 
					(index * letterWidth)%dims[0], 
					((index * letterWidth)/dims[0])*letterHeight, 
					letterWidth, 
					letterHeight, 
					scale, 
					scale, 
					false, false, 0, 0, 0, 0, 0, 0, 1f);
		}
	}

}

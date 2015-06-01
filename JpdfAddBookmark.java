/*
  JpdfAddBookmark

  Kenshi Muto <kmuto@debian.org>

  License:
  Copyright 2013 Kenshi Muto.
  
  Permission is hereby granted, free of charge, to any person obtaining a
  copy of this software and associated documentation files (the "Software"),
  to deal in the Software without restriction, including without limitation
  the rights to use, copy, modify, merge, publish, distribute, sublicense,
  and/or sell copies of the Software, and to permit persons to whom the
  Software is furnished to do so, subject to the following conditions:
  
  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.
  
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
  SOFTWARE IN THE PUBLIC INTEREST, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR
  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  DEALINGS IN THE SOFTWARE.
*/
/* FIXME: Please replace them with com.itextpdf if you have another iText version. */
import java.io.*;
import java.util.*;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;

class ErrMsgException extends Exception {
    // エラークラス
    public ErrMsgException(String s) {
	super(s);
    }
}

public class JpdfAddBookmark {
    protected static char charAt(String s, int i) {
	// 指定位置のcharを返す
	if (s.length() > i) {
	    return s.charAt(i);
	}
	return '\000';
    }

    protected static int roman2arabic(String s, int preample) {
	// ローマ数字からアラビア数字に変換する。
	// もともとアラビア数字の場合はプリアンプル数値を追加する。

	if (s.matches("^\\d+$")) return Integer.parseInt(s) + preample;

	int i = 0;
	int v = 0;
	int n;
	char c;
	s = s.toLowerCase();
	if (s.charAt(i) == 'm') {
	    for (n = 0; charAt(s, i) == 'm'; n++) i++;
	    if (n > 4) return 0;
	    v += n * 1000;
	}
	if (charAt(s, i) == 'd' || charAt(s, i) == 'c') {
	    if ((c = charAt(s, i)) == 'd') {
		i++;
		v += 500;
	    }
	    if (c == 'c' && charAt(s, i + 1) == 'm') {
		i += 2;
		v += 900;
	    } else if (c == 'c' && charAt(s, i + 1) == 'd') {
		i += 2;
		v += 400;
	    } else {
		for (n = 0; charAt(s, i) == 'c'; n++) i++;
		if (n > 4) return 0;
		v += n * 100;
	    }
	}
	if (charAt(s, i) == 'l' || charAt(s, i) == 'x') {
	    if ((c = charAt(s, i)) == 'l') {
		i++;
		v += 50;
	    }
	    if (c == 'x' && charAt(s, i + 1) == 'c') {
		i += 2;
		v += 90;
	    } else if (c == 'x' && charAt(s, i + 1) == 'l') {
		i += 2;
		v += 40;
	    } else {
		for (n = 0; charAt(s, i) == 'x'; n++) i++;
		if (n > 4) return 0;
		v += n * 10;
	    }
	}
	if (charAt(s, i) == 'v' || charAt(s, i) == 'i') {
	    if ((c = charAt(s, i)) == 'v') {
		i++;
		v += 5;
	    }
	    if (c == 'i' && charAt(s, i + 1) == 'x') {
		i += 2;
		v += 9;
	    } else if (c == 'i' && charAt(s, i + 1) == 'v') {
		i += 2;
		v += 4;
	    } else {
		for (n = 0; charAt(s, i) == 'i'; n++) i++;
		if (n > 4) return 0;
		v += n;
	    }
	}
	return (v >= 1 && v <= 4999) ? v : 0;
    }

    public static void usage() {
	// 使い方
	System.out.println("JpdfAddBookmark (c)2013-2014 Kenshi Muto");
	System.out.println("java -classpath /usr/share/java/itext.jar:. JpdfAddBookmark InPDF_filename OutPDF_filename bookmark_filename [preample_offset# [foldlevel]]");
    }

    public static void main(String args[]) {
	// メインルーチン
	if (args.length < 3 || args.length > 5) {
	    usage();
	    return;
	}

	// ブックマークツリーを作成する
	ArrayList<HashMap> bookmark = new ArrayList<HashMap>();
	HashMap<String, Object> lastmap = null;
	ArrayDeque<ArrayList> queue = new ArrayDeque<ArrayList>();
	queue.push(bookmark);

	// しおりファイルの読み込み
	try {
	    File file = new File(args[2]);
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    String line;
	    int previouslevel = 0;
	    int lno = 0;

	    while((line = br.readLine()) != null) {
		lno++;
		if (line.startsWith("#")) continue; // コメント記号#をスキップ

		int level = 0;
		if (line.startsWith("\t")) {
		    // レベル調査
		    do {
			line = line.substring(1);
			level++;
		    } while(line.startsWith("\t"));
		}

		String lines[] = line.split("\t");
		line = "";
		if (lines.length > 2) {
		    for (int i = 0; i < lines.length - 1; i++) {
			line += lines[i];
			if (i < lines.length - 1) line += "　";
		    }
		} else {
		    line = lines[0];
		}
		if (lines.length < 2) continue;

		int preample = 0;
		if (args.length >= 4) {
		    preample = Integer.parseInt(args[3]);
		}
		int page = roman2arabic(lines[lines.length - 1], preample);

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("Title", line);
		map.put("Action", "GoTo");
		map.put("Page", page + " Fit");

		if (args.length >= 5) {
		    // ブックマークを閉じるか
		    int foldlevel = Integer.parseInt(args[4]);
		    if (foldlevel <= level) map.put("Open", "false");
		}

		if (level > previouslevel) {
		    if (level - previouslevel > 1) {
			// ジャンプしすぎ
			throw new ErrMsgException(lno + "行: 前の行より2つ以上深い階層になっています。");
		    }
		    // スタックを掘る
		    ArrayList<HashMap> kids = new ArrayList<HashMap>();
		    kids.add(map);
		    if (lastmap != null) {
			lastmap.put("Kids", kids);
		    } else {
			throw new ErrMsgException(lno + "行: 最初の有効行がタブから始まっています。");
		    }
		    queue.push(kids);

		} else if (level < previouslevel) {
		    // スタックをポップ
		    for (int i = previouslevel; i > level; i--) {
			queue.pop();
		    }
		    queue.peek().add(map);
		} else {
		    // 同じレベルならそのまま入れる
		    queue.peek().add(map);
		}
		lastmap = map;
		previouslevel = level;
	    }
	    
	    br.close();
	} catch(IOException e) {
	    System.out.println(e);
	    return;
	} catch(ErrMsgException e) {
	    System.out.println("エラーが発生しました。\n" + e.getMessage());
	    return;
	}

	// PDFの読み書き
	try {
	    PdfReader reader = new PdfReader(args[0]);
            int maxpage = reader.getNumberOfPages();

	    OutputStream os = new FileOutputStream(args[1]);
	    PdfCopyFields copyFields = new PdfCopyFields(os);
	    copyFields.addDocument(reader);
	    
	    copyFields.setOutlines(bookmark);

	    if (args.length > 3) { // 前付
		PdfPageLabels labels = new PdfPageLabels();
		if (maxpage > 0)
		    labels.addPageLabel(1, PdfPageLabels.LOWERCASE_ROMAN_NUMERALS, null, 1);
		if (maxpage > Integer.parseInt(args[3]))
		    labels.addPageLabel(Integer.parseInt(args[3]) + 1, PdfPageLabels.DECIMAL_ARABIC_NUMERALS, null, 1);
		
		copyFields.getWriter().setPageLabels(labels);
	    }

	    copyFields.close();
	    os.close();
	} catch (DocumentException e) {
	    System.out.println(e);
	    return;
	} catch (IOException e) {
	    System.out.println(e);
	    return;
	}
	System.out.println(args[1] + " を作成しました。");
    }
}

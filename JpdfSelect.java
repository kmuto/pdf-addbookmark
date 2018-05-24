/*
  JpdfSelect

  PDF page extractor

  Copyright 2018 Kenshi Muto

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
import java.io.*;
import java.util.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.parser.*;

class ErrMsgException extends Exception {
    // エラークラス
    public ErrMsgException(String s) {
        super(s);
    }
}

public class JpdfSelect {
    protected static char charAt(String s, int i) {
	// 指定位置のcharを返す
	if (s.length() > i) {
	    return s.charAt(i);
	}
	return '\000';
    }

    protected static String arabic2roman(int n) {
	String v = "";
	String i[] = {"", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix"};
	String x[] = {"", "x", "xx", "xxx", "xl", "l", "lx", "lxx", "lxxx", "xc"};
	String c[] = {"", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm"};
	String m[] = {"", "m", "mm", "mmm", "mmmm"};
	if (n >= 1 && n <= 4999) {
	    v += m[new Double(Math.floor(n / 1000)).intValue()];
	    n %= 1000;
	    v += c[new Double(Math.floor(n / 100)).intValue()];
	    n %= 100;
	    v += x[new Double(Math.floor(n / 10)).intValue()];
	    n %= 10;
	    v+= i[n];
	}
	return v;
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
	System.out.println("JpdfSelect (c)2018 Kenshi Muto");
	System.out.println("java -classpath /usr/share/java/itext.jar:. JpdfSelect 入力PDF 前置文字列 ページ範囲");
	System.out.println("前置文字列+ページ.pdf が生成される。ページ範囲は,で区切り、-で範囲指定できる");
    }

    public static void main(String args[]) {
	// メインルーチン
	if (args.length != 3) {
		usage();
		return;
	}

	List<String> nmbls = new ArrayList<>(Arrays.asList(new String[]{}));

	String parray[] = args[2].split(",");
	for (String ps: parray) {
	    if (ps.contains("-")) {
		String p2array[] = ps.split("-");
		if (p2array[0].matches("^[0-9]+$") && p2array[1].matches("^[0-9]+$")) {
		    // アラビア数字
		    for (int i2 = Integer.parseInt(p2array[0]); i2 <= Integer.parseInt(p2array[1]); i2++) {
			nmbls.add(Integer.toString(i2));
		    }
		} else {
		    // ローマ数字
		    for (int i2 = roman2arabic(p2array[0], 0); i2 <= roman2arabic(p2array[1], 0); i2++) {
			nmbls.add(arabic2roman(i2));
		    }
		}
	    } else {
		// そのまま利用
		nmbls.add(ps);
	    }
	}

	System.out.println("以下のページを抽出:" + String.join(", ", nmbls));

	try {
	    PdfReader reader = new PdfReader(args[0]);
	    PdfPageLabels labels = new PdfPageLabels();
	    String docnmbls[] = labels.getPageLabels(reader);

	    for (int i = 0; i < reader.getNumberOfPages(); i++) {
		if (nmbls.contains(docnmbls[i])) {
		    Document document = new Document(reader.getPageSizeWithRotation(1));
		    PdfCopy writer = new PdfCopy(document, new FileOutputStream(args[1] + "-p" + docnmbls[i] + ".pdf"));
		    
		    document.open();
		    PdfImportedPage page = writer.getImportedPage(reader, i + 1);
		    writer.addPage(page);
		    document.close();
		    writer.close();
		}
	    }
	} catch (DocumentException e) {
	    System.out.println(e);
	} catch (IOException e) {
	    System.out.println(e);
	}
    }
}

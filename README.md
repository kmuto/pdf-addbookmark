# PDFに外部目次ファイルからしおり(ブックマーク)を埋め込むツール (Javaバージョン)

Copyright 2012-2013 Kenshi Muto <kmuto@kmuto.jp>

既存のPDFに対してしおりを埋め込むには、通常、AcrobatのようなGUIソフトウェア
を利用して1つずつ手で設定していくことになりますが、数が多いとうんざりする作業
になります。

本ツールは、別途抽出した目次テキストファイルなどを少し加工したものを使い、
しおり情報をPDFに一度に埋め込むためのものです。

## 必要なソフトウェア
- Java / OpenJDK: http://openjdk.java.net/
- iText Library: http://itextpdf.com/

Debian GNU/Linux 7.0 Wheezyで動作確認をしています。
apt-get install openjdk-6-jdk libitext-java でパッケージをインストールします。

## コンパイル
次のようにコンパイルします。

```
javac -classpath /usr/share/java/itext.jar JpdfAddBookmark.java
```

コンパイラが「com.lowagie.text.* が見つからない」旨のエラーを返した場合は、
JpdfAddBookmark.javaの宣言行を次のように変更してください。

```
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
```

コンパイルされたクラスファイル JpdfAddBookmark.class と ErrMsgException.class
を適当なディレクトリにコピーしてください。

そして、jpdfaddbookmark.sh の JPDFPATH=. となっている箇所をそのディレクトリ
に変更してください。

```
例: JPDFPATH=/usr/local/jpdfaddbookmark
```

jpdfaddbookmark.shをパスの通ったところにコピーしてください。

## 使い方
しおり用の目次ファイルを作成し、jpdfaddbookmark.sh にPDFファイルと共に
指定して、しおり埋め込みの新しいPDFを作成します。

埋め込み対象のPDFは、大扉から白ページまですべて込みの完全な状態であるこ
とを想定しています。

書籍によっては、大扉ノンブルをアラビア数字の1とするいわゆる「通しページ」
ではなく、1章が始まるまではローマ数字1(i)で始めて1章からアラビア数字の
1とするものもあります。この場合、アラビア数字1が始まるまで(いわゆる前
付)のページ総数を、オプションで指定します。

```
jpdfaddbookmark.sh  元PDFファイル  新PDFファイル しおり目次ファイル 前付ページ数  デフォルトで畳む数
```

* デフォルトで畳む数 は、ここで指定した数以上の階層のものについて畳んだ状態のしおりになります。
* 「前付ページ数」、「デフォルトで畳む数」はオプションです。通しページで、かつデフォルトで畳む数を指定したいときには、前付ページ数 に「0」を指定してください。

### 例
- jpdfaddbookmark.sh in.pdf out.pdf bookmark.txt ←大扉1から始まる通しページのPDF in.pdfにbookmark.txtのしおりを埋め込んで、out.pdfとして書き出す
- jpdfaddbookmark.sh in_with_mae.pdf out.pdf bookmark.txt 22 ←前付ページ総数は22ページ分(i〜xxii)のin_with_mae.pdfに、bookmark.txtのしおりを埋め込んで、out.pdfとして書き出す

### しおり用の目次ファイル
- 内部での自動変換は行いますが、文字化け等を防ぐため、UTF-8エンコーディング、UNIX改行形式(LF)で記述することを推奨します。
- 先頭のタブの数で見出しの深さを指定します。この深さに基づいて、しおりの階層構造ができあがります。(なお、前の行よりも2つ以上タブが多いと階層構造を作れないのでエラーを報告し、中止します。)
- 見出しとノンブルは、タブで区切ります。(ノンブルの前のタブは複数あっても1つとして扱われます。)
- #から始まる行はコメントです。
- 空行は単に無視されます。

例を次に示します。

```
# 『初めてのPerl 第6版』(オライリー・ジャパン) より。
# 前付部は28ページ分。
訳者まえがき	v
はじめに	vii
1章　Perl入門	1
	1.1　質疑応答	1
		1.1.1　この本はわたしに向いていますか?	1
		1.1.2　なぜこんなにたくさんの脚注が付いているのでしょうか?	2
		1.1.3　練習問題と解答について教えてください	3
		1.1.4　練習問題の先頭の数字は何を意味するのでしょうか?	4
		1.1.5　私はPerlコースの講師ですが、アドバイスをいただけますか?	4
	1.2　Perlとは何の略でしょうか?	5
		1.2.1　なぜLarryはPerlを創ったのでしょうか?	5
		1.2.2　なぜLarryはほかの言語を使わなかったのでしょうか?	5
		1.2.3　Perlは易しいでしょうか難しいでしょうか?	6
		1.2.4　Perlはどのようにしてこんなに人気を得るようになったのですか?	8
		1.2.5　いまPerlに何が起こっているのでしょうか?	9
  2章　スカラーデータ	25
	2.1　数値	25
```

## Dockerイメージ
https://hub.docker.com/r/kenshimuto/jpdfaddbookmark でDockerイメージを提供しています。

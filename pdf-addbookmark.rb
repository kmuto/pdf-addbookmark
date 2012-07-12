#!/usr/bin/env ruby
# encoding: utf-8
# pdf-addbookmark.rb: TOC text to PDF bookmark PostScript converter / PDF embedder
# Kenshi Muto <kmuto@debian.org>
#
# License:
# Copyright 2012 Kenshi Muto.
# 
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
# SOFTWARE IN THE PUBLIC INTEREST, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR
# OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
# ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.
# 
#  Requirements: Ruby 1.9
#  Optional: Ghostscript
#
## Original source and idea:
## toc2bkmark.py: TOC text PDF bookmark file converter
## Akihiro Takizawa <akihiro.takizawa@gmail.com>
## https://gist.github.com/3096577

require 'optparse'

def conv_to_oct(source)
  buf = ""
  source.encode("UTF-16BE", "UTF-8").each_codepoint do |c|
    h = c >> 8 # high/low bit
    l = c & 255
    buf += "\\#{h.to_s(8)}\\#{l.to_s(8)}"
  end

  "\\376\\377#{buf}" # with BOM
end

def build_mark(title, page_in_pdf, count)
  # FIXME: Python版ではtitleの0x3000→' 'の変換あり。理由は?
  if !count.nil? && count != 0
    "[/Title (#{conv_to_oct(title)}) /Count #{count} /Page #{page_in_pdf} /OUT pdfmark"
  else
    "[/Title (#{conv_to_oct(title)}) /Page #{page_in_pdf} /OUT pdfmark"
  end
end

def roman2arabic(roman, forewordpages)
  return roman.to_i if roman =~ /\A\d+\Z/

  i = 0
  v = 0
  n = 0
  c = 0
  roman.downcase!
  if roman[i] == 'm'
    0.upto(5) do |j|
      n = j
      break unless roman[i] == 'm'
      i += 1
    end
    raise "invalid roman numeric: #{roman}" if n > 4
    v += n * 1000
  end
  if roman[i] == 'd' || roman[i] == 'c'
    if (c = roman[i]) == 'd'
      i += 1
      v += 500
    end
    if c == 'c' && roman[i + 1] == 'm'
      i += 2
      v += 900
    elsif c == 'c' && roman[i + 1] == 'd'
      i += 2
      v += 400
    else
      0.upto(5) do |j|
        n = j
        break unless roman[i] == 'c'
        i += 1
      end
      raise "invalid roman numeric: #{roman}" if n > 4
      v += n * 100
    end
  end
  if roman[i] == 'l' || roman[i] == 'x'
    if (c = roman[i]) == 'l'
      i += 1
      v += 50
    end
    if c == 'x' && roman[i + 1] == 'c'
      i += 2
      v += 90
    elsif c == 'x' && roman[i + 1] == 'l'
      i += 2
      v += 40
    else
      0.upto(5) do |j|
        n = j
        break unless roman[i] == 'x'
        i += 1
      end
      raise "invalid roman numeric: #{roman}" if n > 4
      v += n * 10
    end
  end

  if roman[i] == 'v' || roman[i] == 'i'
    if (c = roman[i]) == 'v'
      i += 1
      v += 5
    end
    if c == 'i' && roman[i + 1] == 'x'
      i += 2
      v += 9
    elsif c == 'i' && roman[i + 1] == 'v'
      i += 2
      v += 4
    else
      0.upto(5) do |j|
        n = j
        break unless roman[i] == 'i'
        i += 1
      end
      raise "invalid roman numeric: #{roman}" if n > 4
      v += n
    end
  end

  v - forewordpages
end

def main
  forewordpages = 0
  splitter = "　"
  hidelevel = 2 # hide subsection and smaller headers by default
  inpdf = nil
  outpdf = nil
  bookmarkoutput = nil
  gs = "gs -q -dBATCH -dNOPAUSE -sDEVICE=pdfwrite -sOutputFile=<<%outpdf%>> <<%bookmark%>> <<%inpdf%>>"

  marklist = []

  parser = OptionParser.new
  parser.banner = "Usage: #{File.basename($0)} < toc.txt"
  parser.on('-f', '--forewordpages=NUMBER', 'Specify foreword offset count (number).') {|n| forewordpages = n.to_i }
  parser.on('-s', '--splitter=STRING', 'Specify alternative character for tab in titles.') {|s| splitter = s }
  parser.on('-l', '--hidelevel=NUMBER', 'Specify header level should be folded by default. (default=2)') {|l| hidelevel = l.to_i }
  parser.on('-g', '--gs=STRING', 'Specify Ghostscript caller with parameters.') {|g| gs = g }
  parser.on('-i', '--input=PDFFILE', 'Specify input PDF filename (only works with -o option.)') {|o| inpdf = o }
  parser.on('-o', '--output=PDFFILE', 'Specify output PDF filename (only works with -i option.)') {|o| outpdf = o }
  parser.on('-h', '--help', 'Print this messages.') do
    puts parser.help
    exit 0
  end

  begin
    parser.parse!
  rescue OptionParser::ParseError => err
    puts err.message
    STDERR.puts parser.help
    exit 1
  end

  if !inpdf.nil? && !outpdf.nil?
    if inpdf == outpdf
      STDERR.puts "You can't override original file. Specify other output filename."
      exit 1
    end

    bookmarkoutput = outpdf + "__" + $$.to_s
    gs = gs.gsub('<<%outpdf%>>', outpdf).gsub('<<%bookmark%>>', bookmarkoutput).gsub('<<%inpdf%>>', inpdf)
  end

  ARGF.each_line do |l|
    if l =~ /\A\#/ # comment
      forewordpages = $1.to_i if l =~ /\A\#\s*FOREWORDPAGES:\s*(\d+)/
      splitter = $1 if l.chomp =~ /\A\#\s*SPLITTER:(.+)/
      hidelevel = $1.to_i if l.chomp =~ /\A\#s\*HIDELEVEL:\s*(\d+)/
      next
    end
    level = 0
    l.sub!(/\A\t+/) do |m|
      level = m.length
      ''
    end
    strs = l.chomp.split(/\t/)
    page = roman2arabic(strs.pop, forewordpages)
    title = strs.join(splitter)

    marklist.push({ 'title' => title,
                    'page' => page,
                    'level' => level
                  })
  end

  bookmarkio = nil
  unless bookmarkoutput.nil?
    bookmarkio = File.open(bookmarkoutput, "w") # FIXME: use tmpfile
  end

  marklist.each_with_index do |mark, i|
    count = 0
    (i + 1).upto(marklist.size - 1) do |j|
      count += 1 if marklist[j]['level'] > mark['level']
      break if marklist[j]['level'] <= mark['level']
    end
    count = -count if hidelevel <= mark['level']

    if bookmarkio.nil?
      puts build_mark(mark['title'], mark['page'] + forewordpages, count)
    else
      bookmarkio.puts build_mark(mark['title'], mark['page'] + forewordpages, count)
    end
  end

  unless bookmarkio.nil?
    bookmarkio.close
    fork {
      exec(gs)
    }
    Process.waitall
    File.unlink(bookmarkoutput)
  end

end

main

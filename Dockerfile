FROM debian:buster-slim
LABEL maintainer="kmuto@kmuto.jp"

ENV LANG en_US.UTF-8

# setup
RUN apt-get update && \
    mkdir -p /usr/share/man/man1 && \
    apt-get install -y --no-install-recommends \
      locales git-core curl ca-certificates libitext-java openjdk-11-jdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen
RUN locale-gen en_US.UTF-8 && update-locale en_US.UTF-8

RUN git clone https://github.com/kmuto/pdf-addbookmark.git && \
  cd pdf-addbookmark && \
  javac -classpath /usr/share/java/itext.jar JpdfAddBookmark.java && \
  cp JpdfAddBookmark.class ErrMsgException.class /usr/local/bin && \
  cp jpdfaddbookmark.sh /usr/local/bin && \
  sed -i -e "s/JPDFPATH=./JPDFPATH=\/usr\/local\/bin/" /usr/local/bin/jpdfaddbookmark.sh && \
  chmod a+x /usr/local/bin/jpdfaddbookmark.sh

VOLUME ["/work"]
WORKDIR /work

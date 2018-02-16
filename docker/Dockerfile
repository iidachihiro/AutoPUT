FROM ubuntu:16.04
MAINTAINER Keita Tsukamoto <t.k.columbus1012@gmail.com>

# install dependencies
RUN apt-get update
RUN apt-get install apt-file git
RUN apt-file update
RUN apt-get install software-properties-common
RUN add-apt-repository ppa:webupd8team/java
# install oracle java
RUN apt-get update
RUN apt-get install oracle-java8-installer
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
# install txl
ADD 10829-txl10.6e.linux64.tar.gz /tmp/SourcererCC/10829-txl10.6e.linux64.tar.gz
WORKDIR /tmp/
RUN tar xvzf 10829-txl10.6e.linux64.tar.gz
WORKDIR /tmp/txl10.6e.linux64
RUN ./InstallTxl
ENV PATH /root/bin:$PATH
# install SourcererCC
ADD SourcererCC /root/


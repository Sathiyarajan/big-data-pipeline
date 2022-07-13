# big-data-e2e

### entire steps got automated

Prerequisites: Ubuntu 18.04 LTS version is needed.  
### install java and ssh  

```
sudo apt install open-vm-tools-desktop git vim net-tools -y

sudo apt install openjdk-8-jre-headless
```
### for installing ssh

```
sudo apt-get install openssh-server openssh-client

ssh-keygen -t rsa -P ""

cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

ssh localhost

```

### get the components from the git repo

```
cd /opt/

git clone https://github.com/Sathiyarajan/big-data-e2e.git

cp -r big-data-e2e/* .
```


### step 4 : paste the following contents in /root/.bashrc.

```
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

export ZOOKEEPER_HOME=/opt/zookeeper/zookeeper-3.4.10
export ZOOKEEPER_CONF_DIR="$ZOOKEEPER_HOME/conf"
export ZOOKEEPER_CLASSPATH="$ZOOKEEPER_CONF_DIR"
export PATH=$PATH:$ZOOKEEPER_HOME/bin

export HADOOP_HOME=/opt/hadoop/hadoop-2.7.3
export HADOOP_INSTALL=$HADOOP_HOME
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
export PATH=$PATH:$HADOOP_HOME/sbin:$HADOOP_HOME/bin

export SQOOP_HOME=/opt/sqoop/sqoop-1.4.6.bin__hadoop-2.0.4-alpha
export PATH=$PATH:$SQOOP_HOME/bin

export HBASE_HOME=/opt/hbase/hbase-1.2.10
export PATH=$PATH:$HBASE_HOME/bin

export SPARK_HOME=/opt/spark/spark-2.2.1-bin-hadoop2.7
export PATH=$SPARK_HOME/bin:$PATH

export HIVE_HOME=/opt/hive/apache-hive-1.2.1-bin
export PATH=$PATH:$HIVE_HOME/bin

export HCAT_HOME=$HIVE_HOME/hcatalog

export PIG_HOME=/opt/pig/pig-0.16.0
export PATH=$PATH:/opt/pig/pig-0.16.0/bin

export FLUME_HOME=/opt/flume/apache-flume-1.9.0-bin
export PATH=$PATH:$FLUME_HOME/bin/

alias start_hadoop=/opt/hadoop/hadoop-2.7.3/sbin/start-all.sh
alias stop_hadoop=/opt/hadoop/hadoop-2.7.3/sbin/stop-all.sh
alias start_hive=/opt/hive/apache-hive-1.2.1-bin/bin/hive
alias spark_shell=$SPARK_HOME/bin/spark-shell
alias spark_submit=$SPARK_HOME/bin/spark-submit

```

### start all the services with appropriate commands.

### to setup in windows use ubuntu from app store and install it and setup the big data environment by following the above steps.

### Skewed Joins

https://medium.com/expedia-group-tech/skew-join-optimization-in-hive-b66a1f4cc6ba


### important links

hadoop: https://data-flair.training/blogs/install-hadoop-on-single-machine/

hive: https://data-flair.training/blogs/apache-hive-installation/

spark: https://data-flair.training/blogs/install-spark-ubuntu/

pig: https://data-flair.training/blogs/apache-pig-installation-ubuntu-tutorial/

# HDFS hacks

```
hadoop fs -ls /tmp/data | sed '1d;s/  */ /g' | cut -d\  -f8
```
# linux hacks

```
#find string with special characters
grep -vrnw . -e "TEST_"

# find and replace with backupfile creation
find . -name '*.SQL' |xargs perl -pi -e 's/TEST1_/TEST_/g'

# delete matching multiple files in a sub directories
find . -name \*.bak -type f -delete


# find file which is more than 50MB in size in a direcroty
find . -type f -size +5M -exec ls -lh {} \; | awk '{print $9 "|| Size :" $5}'

# find a file which created recently
find . -type f -mmin -5

#find a top 10 files in size
find . -type f -printf "%s\t%p\n" | sort -nr | head -10

#find a top 10 directories in size
alias ducks='du -cks * | sort -rn | head'
du -a . | sort -n -r | head

#List overall file size in HDFS:
hadoop fs -du -s -h /hdfs/path/

#List only files and directories in HDFS:
hadoop fs -count -v -h /hdfs/path/ (human readable GB, MB)
hadoop fs -count -v /hdfs/path (give the exact count on directory,files)

#List only files in HDFS:
hadoop fs -ls -R /hdfs/path/* | grep -v '^d' | wc -l --> list only files
```

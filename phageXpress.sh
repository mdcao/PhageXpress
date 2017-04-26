#!/bin/bash

dir=`dirname $0`

java -cp $dir/phageXpress.jar:$dir/libs/htsjdk-2.9.1-3.jar:$dir/libs/japsa-dev.jar:$dir/libs/slf4j-api-1.7.25.jar:$dir/libs/slf4j-simple-1.7.25.jar japsa.phage.PhageXpressCmd $@



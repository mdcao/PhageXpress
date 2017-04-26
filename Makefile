#############################################################################
# Copyright (c) Minh Duc Cao, Monash Uni & UQ, All rights reserved.         #
#                                                                           #
# Redistribution and use in source and binary forms, with or without        #
# modification, are permitted provided that the following conditions        #
# are met:                                                                  # 
#                                                                           #
# 1. Redistributions of source code must retain the above copyright notice, #
#    this list of conditions and the following disclaimer.                  #
# 2. Redistributions in binary form must reproduce the above copyright      #
#    notice, this list of conditions and the following disclaimer in the    #
#    documentation and/or other materials provided with the distribution.   #
# 3. Neither the names of the institutions nor the names of the contributors#
#    may be used to endorse or promote products derived from this software  #
#    without specific prior written permission.                             #
#                                                                           #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   #
# IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, #
# THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    #
# PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         #
# CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     #
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       #
# PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        #
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    #
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      #
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        #
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              #
############################################################################/


############################################################################
# A generic makefile for java
#  
############################################################################


###########################################################################
# Modify the following parameter to suit your setting

#Class directory
CLASS_DIR=target/main-classes
#Source directory
SRC_DIR=src/main/java

#Java compiler
JCC=javac

#target jar file
JAR_FILE=phageXpress.jar

#External library directory
LIB_DIR=libs

#List of external libraries
EXT_LIBS=htsjdk-2.9.1-3.jar japsa-dev.jar slf4j-api-1.7.25.jar slf4j-simple-1.7.25.jar

###########################################################################
##What this scripts does:
# 1. Find all packages by searching SRC_DIR subdirectories
# 2. Get all the java file, and compile them to the CLASS_DIR directory
# 5. Clean back up files (*.bak, ~, etc)
# To be implemented:
# 3. Copy all the resources to class directory
# 4. Make java file
##########################################################################

COMMA:= ,
EMPTY:=
SPACE:= $(SPACE) $(SPACE)
LIBS:=$(subst $(SPACE),:, $(addprefix $(LIB_DIR)/, $(EXT_LIBS)))

##Get all the packages in $(SRC_DIR). 
PACKAGE_DIRS := $(shell echo `cd $(SRC_DIR);find . -type d`)
SRC_DIRS := $(addprefix $(SRC_DIR)/, $(PACKAGE_DIRS))

##For each package directory, get its equivalent class directory
CLASS_DIRS := $(addprefix $(CLASS_DIR)/, $(PACKAGE_DIRS))

##Get the list of java files
JAVA_FILES  := $(foreach dir,$(SRC_DIRS),$(wildcard $(dir)/*.java))
CLASS_FILES := $(subst $(SRC_DIR)/,$(CLASS_DIR)/,$(JAVA_FILES:.java=.class))


#####################Make targets 

VPATH=$(subst ' ',':',$(PACKAGE_DIRS)) 
$(CLASS_DIR)/%.class: $(SRC_DIR)/%.java $(CLASS_DIR)
	$(JCC) -sourcepath $(SRC_DIR) -cp $(CLASS_DIR):$(LIBS) -nowarn -d $(CLASS_DIR) $(JDEBUGFLAGS) $<


all: jar

classes:  $(CLASS_FILES)


###Create the class directory if neccessary
$(CLASS_DIR):
	mkdir -p $(CLASS_DIR)

$(JAR_FILE):classes
	jar cf $(JAR_FILE) -C $(CLASS_DIR) . 

jar: $(JAR_FILE)

clean:
	@@for i in $(PACKAGE_DIRS); do \
		echo "rm -f $(SRC_DIR)/$$i/*.bak $(SRC_DIR)/$$i/*.class $(SRC_DIR)/$$i/*~ $(CLASS_DIR)/$$i/*.class"; \
		rm -f $(SRC_DIR)/$$i/*.bak $(SRC_DIR)/$$i/*.class $(SRC_DIR)/$$i/*~ $(CLASS_DIR)/$$i/*.class; \
	done


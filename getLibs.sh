#!/usr/bin/env bash

cd `dirname $0`

echo "Preparing libraries directory..."
rm -r libs
mkdir libs
cd libs

set -e

echo "Getting libs..."

echo -e "Getting \"JArgParse-1.0.jar\"..."
cp ../../JArgParse/build/jar/JArgParse-1.0.jar JArgParse-1.0.jar

echo -e "Getting \"JArgParse-1.0-sources.zip\"..."
cp ../../JArgParse/build/jar/JArgParse-1.0-sources.zip JArgParse-1.0-sources.zip

echo -e "Getting \"Universal-Tokenizer-1.0.jar\"..."
cp ../../Universal-Tokenizer/build/jar/Universal-Tokenizer-1.0.jar Universal-Tokenizer-1.0.jar

echo -e "Getting \"Universal-Tokenizer-1.0-sources.zip\"..."
cp ../../Universal-Tokenizer/build/jar/Universal-Tokenizer-1.0-sources.zip Universal-Tokenizer-1.0-sources.zip

echo "Adding libraries to Git..."
git add *

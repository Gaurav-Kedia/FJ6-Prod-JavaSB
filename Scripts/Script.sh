#!/bin/bash

echo "********** Running Script .sh file **********"

# Change the directory to your SB project

cd /home/ubuntu/Java-SB-1/FJ6-Prod-JavaSB
pwd
ls -lh

# Receive tag reference as input parameter
tag_ref="$1"

if [ -z "$tag_ref" ]; then
  echo "Error: Tag reference is empty. Exiting..."
  exit 1
fi

# Fetch the latest changes from the remote repository
git fetch

# Checkout the specified tag
git checkout "$tag_ref"

echo "cleaning ang building with mvn"
mvn clean install

cd target
echo "Inside Target folder"
ls -lh

nohup java -jar FJ6-Prod-JavaSB-0.0.1.jar &

echo "********** End of Deploy Script .sh file 1 **********"

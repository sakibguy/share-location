﻿#!/usr/bin/env bash
##################################################
### Script : ci.sh (14-10-2017)                ###
### CI     : Continious Integration            ###
### Target : Tool dev			               ###
###	     Automated Continious Integration      ###
###	     among local & remote repo	           ###
##################################################




###############################
### Setting up a repository ###
###############################





############
# Step: 01 #
############

echo
echo [ Done ]  Initializing who am I.
git config --global user.email "sakib.rahman.0000@gmail.com"
git config --global user.name "Sakib Rahman"


############
# Step: 02 #
############

echo [ Done ]  Initializing a new Git repo for this project.
echo
git init


############
# Step: 03 #
############

echo
echo [ Done ]  Saving changes to the repository.
git add .


############
# Step: 04 #
############

echo [ Done ]  Committing...
echo
#############################
## follow up simple commit ##
#############################
## Rename pre_filename.md to new_filename.md
## Create file.ext
## Update file.ext
## Delete file.ext
## Release v1.5.0
## Solved issues
## Default: Commit skipped|forgotten

## 📗 Completion release 1.3.0
## 📗 UPLOAD cr1.5.0
## Dependencies: multiple markers + firebase crud

# 2018.Apr15
## 📗 UPLOAD cr1.6.0
## 📗 UPLOAD DEBUGGER Debugger importance
## 📗 UPLOAD 7h later eureka!
## 📗 UPLOAD cr2018.Apr15.1.7.0
## 📗 UPLOAD changes for doc

# 2018.Apr18
## UPLOAD Alpha:marketing rule
## 💚 UPLOAD cr2018.Apr18.1.8.0
## UPLOAD changes
## UPLOAD screenshots, pre-launch report
## UPLOAD todo for 2018.Apr18.1.9.0
## 💚 UPLOAD cr2018.Apr18.1.9.0

# 2018.Apr21
## ADDED feature, camera focus point (black box)
## 💚 UPLOAD cr2018.Apr21.1.10.0
## UPLOAD pre-launch report

# 2018.Apr22
## 💚 UPLOAD cr2018.Apr21.1.10.1

# 2018.Apr23
## 💚 UPLOAD cr2018.Apr23.1.11.0

# 2018.Apr25
## 🔴 UPLOAD changes: unstable  2018.Apr25.1.12.0

# 2018.Apr27
## 💚 UPLOAD research: rendering, realtime sys, marketing
## CREATE look-and-feel dir with contents

# 2018.Apr28
## 💚 UPLOAD cr2018.Apr28.1.12.0

# 2018.Apr30
## 💚 UPLOAD cr2018.Apr30.1.12.1

# 2018.May4
## UPLOAD block diagram for cr2018.May4.1.13.0

# 2018.May6
## 💚 UPLOAD cr2018.May6.1.13.0

# 2018.May9
## 💚 UPLOAD cr2018.May9.1.14.0

# 2018.May12
## UPLOAD stable release: TabLayout with PageViewer & UX+UI+ReceivedFriends
## UPLOAD functional friends: (stable release 2)
## 💚 UPLOAD cr2018.May12.1.15.0

# 2018.May13
## 💚 UPLOAD cr2018.May13.1.16.0

# 2018.May14
## 💚 UPLOAD cr2018.May14.1.17.0

# 2018.May15
## 💚 UPLOAD cr2018.May15.1.18.0, ATTRACTIVE to ENGAGE

# 2018.May17
## ADD onClickWave to engage

git commit -m "ADD onClickWave to engage"

############
# Step: 05 #
############

# List your existing remotes in order to get the name of the remote you want to change.
echo
echo [ Done ]  checking remote origin.
echo
git remote -v

############
# Step: 06 #
############

# Change your remote's URL from SSH to HTTPS with the git remote set-url command.
echo
echo [ Done ]  Updating remote URL.
git remote set-url origin https://github.com/Sakib-Rahman-Bangladesh/share-location
############
# Step: 07 #
############

# Verify that the remote URL has changed.
echo [ Done ]  Verifying remote URL.
echo
git remote -v

############
# Step: 08 #
############

echo
echo 08. Pushing local codebase to remote repo...Repo-to-repo collaboration: git push
echo
git push origin master
git push --all -f https://github.com/Sakib-Rahman-Bangladesh/share-location
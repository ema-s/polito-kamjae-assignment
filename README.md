## Setting Up
First download Git from [here](https://git-scm.com). You can find full documentation about Git [here](https://git-scm.com/doc) as well, but I'll give you a quick rundown. BTW from here on I'll be assuming we're all working with Java.

### Cloning the repository
After installing Git check the repository page (if you're reading this online, you probably are in the right place). Click on the green button in the top right, "Clone or download", and copy the URL link for HTTPS mode (you can also use SSH if you already know how to set up your public keys in GitHub). Now find some folder where to set your local repository, and run the following command:

    git gui
    
Choose "Clone Existing Repository", then paste the URL you got from GitHub in the Source Location, and choose a destination folder. You'll be prompted for your GitHub account data.

### Branches
A branch is a different view of the same project on which different features are developed. People usualy work on different local branches which will then be "merged" into the master branch. I'm setting up development branches for everyone so we can keep things tidy, while I'll administrate the master branch and the merging. Please do NOT attempt to work on other branches as uploads to GitHub will be blocked. To access your branch simply type (or use the "git gui" if you prefer):

    git checkout dev-yourfirstname
    
Git will do its magic and prepare a custom version of the project just for you. Only you will be working on this version and you won't affect, nor be affected by the others.

To keep the branch up-to-date with the latest changes from the master branch you need to perform a merge.

    git merge origin/master
    
This command will download all commits (see further down) from the master branch and apply them to your local branch. As it usually happens, conflicts may arise if someone modified heavily something you've been working on. In this case you must either force-accept your version, the master branch's version or create a custom modified version. I usually prefer to run the "git gui" command to manage conflicts.

### Working on the project
Do your stuff, write code, edit things. Once you're happy and want to "track" your result in git, you must type:

    git add filename
    
this will "prepare" filename to be recorded. There's also a shortcut version

    git add .
    
to add all edited files. Be careful on what you add. After adding all desired files you must "commit" your changes so that they are fully recorded by Git.

    git commit -m "Type a message here describing all your implemented features"
    
A commit is a little "snapshot" of the state of the project at a given time. Commits make up the history of the project. To upload your changes into GitHub simply type:

    git push origin dev-yourfirstname
    
At this point you can go on GitHub and send a "Pull Request" to ask for your changes to be merged into the master branch, so that anyone can review them. 

### Working on different devices

Suppose you've been working home at your project and pushed new commits to the repository. Now you're at the Politecnico with your laptop but your repository is not up-to-date with the latest changes. Supposed you found a working hotspost (good luck) you can run a merge of the remote repository onto your local branch:

    git merge origin/dev-yourfirstname
    
and all recent commits will be downloaded. EZPZ.
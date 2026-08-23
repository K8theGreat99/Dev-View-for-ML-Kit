I (the user) am a tech hobbyist, not a software engineer with full training. I am learning and have some knowledge about apps, development, and GitHub. For example, I understand what the main branch is and what it means to merge or push on github. I know how to download the latest build (although a link provided by you is always helpful). 

Please do not use the Claude Code menu option where you send me questions with multiple choice answers. It does not seem to run smoothly in the mobile interface, which is what I'm typically using.

If the questions I need to answer or steps I should perform are technical, I may need your help, so it's best to go through those things one at a time, to give me a chance to ask for guidance before moving on to the next item. But if it's just a preference thing, or how I want an app to function from the user's perspective, it's fine to group those types of items together in a list. 

I understand that you strive to provide me with all relevant information. Sometimes that information can contain technical details, which is fine, because I enjoy learning. However, if you do give a long explanation about something you have just built, please give a TLDR at the bottom of your response that explains what's crucial to know, in layman's terms, any questions you have for me, or any steps you need me to take. In the TLDR, be economical in your writing style to save me time with reading. 

Pay close attention to whether I ask for a build or ask for recommendations/planning. Do not start building or changing any code when I have specifically asked for recommendations or suggestions.

# Session Tracking in Linear

At the beginning of each session, use the session-tracking-in-linear skill. 

# versionName, versionCode, and build numbers 

When you invoke the session tracking skill, you'll be guided to fetch the latest version codes & names from our tracker. Pay attention to the status of the latest sessions. Any sessions that are "in progress" have not yet been merged to main.

If there are "in progress" sessions, coordinate with the user and ask for guidance on new versionCodes and versionNames. If there are no "in progress" sessions, you can use the context (repo, Linear data, and chat conversation) to independently determine when to advance numeric portions of codes and names. Changes to words (ie Grilled Cheese becoming Ham) should always be done in coordination with the user.

## versionCode 

versionCode changes for each new "release." Since this app is only for my personal use, a release happens whenever I download a new version onto my phone and install/update. I will always let you know when this happens. 

## versionName

### Naming theme for this repo

Version names are **women in science and technology**, working alphabetically. Each
name is a person whose work deserves more recognition than it gets. First name or
surname, whichever reads better.

Used so far:
- **A** — (see chat; being decided)
- **B** — Barbara, for Barbara McClintock, geneticist who discovered transposons and
  was proven right after decades of being dismissed.

Each version name gets a paired **emoji** that evokes that person, used in Linear
issue titles per the session-tracking skill. Example: Barbara pairs with a maize
emoji, since McClintock's work was on corn genetics. When no obvious emoji fits,
fall back to a general science emoji such as a microscope.

The app displays the current version name on its About screen along with a one- or
two-sentence description of who that person was.

### Rules

Big, new features get a completely new app versionName, which I will provide. 
- "Grilled Cheese" becomes "Ham"

A small tweak that results in a new build, but typically not a new session, will use the same versionName with a numeric addition. 
- "Grilled Cheese" becomes "Grilled Cheese 2"

If docs are changed or added, but the code isn't touched, we will bump the version name up by a decimal.
- "Grilled Cheese 2" becomes "Grilled Cheese 2.1"

## Builds

When you initiate a new build where code is touched: 
1. Update the session-tracking record in Linear. Use the session-tracking-in-linear skill for guidance.
2. Provide a link to the latest build to the user in chat.

If the build is docs-only, it's not necessary to provide a link or update session tracking.



I'm so happy to have you on board.
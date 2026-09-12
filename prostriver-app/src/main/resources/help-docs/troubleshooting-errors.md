---
title: Troubleshooting & Errors
slug: troubleshooting-errors
summary: What ProStriver's error messages actually mean and what to do about each one.
questions:
  - i cant log in
  - invalid email or password
  - my otp expired
  - invalid otp
  - i keep getting too many attempts
  - why was i logged out
  - email already registered
  - email not verified
  - topic not found
  - i cant edit my topic
  - my plan is stuck on generating
  - current password is incorrect
  - i already have an active challenge
  - something went wrong
  - my custom reminder pattern is not accepted
---

# Troubleshooting & Errors

This page explains every error message you might actually see in ProStriver, in plain language, along with what to do about it.

## Account & Login

**"Email already registered"** — An account already exists with that email. Try logging in instead, or use Forgot Password if you don't remember your credentials.

**"Invalid email or password"** — One or both don't match our records. Double-check for typos, especially in the email.

**"Email not verified"** — You created an account but haven't completed OTP verification yet. Check your inbox for the verification code, or request a new one if it expired.

**"Please wait before requesting OTP again"** — You've requested a new OTP too soon after the last one. This is a short cooldown to prevent spam — wait a bit and try again.

**"Invalid OTP" / "OTP expired" / "OTP already used"** — The code you entered doesn't match, has expired, or was already used. Request a fresh one and enter it promptly.

**"Too many attempts"** — You've entered an incorrect OTP too many times, and that code is now locked. Request a new one.

**"Current password is incorrect"** — Shown when changing your password — the password you entered as your current one doesn't match. If you've forgotten it entirely, log out and use Forgot Password instead.

**Session expired / logged out unexpectedly** — Sessions refresh automatically in the background, but they do eventually expire after extended inactivity for security. Simply log back in.

**"Could not send email. Please check your email address."** — We tried to send you an email (a verification code or a password reset) and the address was rejected. Check it for typos, and if it looks right, try again in a few minutes — occasionally this is a temporary problem on the mail provider's side rather than anything wrong with your address.

## Profile

**"Full name must be between 2 and 80 characters"** — Your display name is either too short or too long. Pick something within that range.

**"Full name contains invalid characters"** — Your name field accepts letters, spaces, and ordinary name punctuation. Remove any symbols, emoji, or digits and try again.

**"No fields provided to update"** — You submitted a profile update without actually changing anything. Edit at least one field before saving.

## Topics & Revisions

**"Topic not found"** — The topic either doesn't exist or doesn't belong to your account. If you're sure it should exist, try refreshing your topic list.

**"Archived topic cannot be updated"** — You're trying to edit a topic you've archived. Unarchive it first if you need to make changes.

**"Provide either revisionPlanId or manualReminderPattern, not both"** — When setting up a topic's revision schedule, choose one scheduling method, not both at once.

**"Invalid manualReminderPattern"** errors (must be comma-separated integers, days must be 1 or greater, cannot be empty, or exceeds the maximum number of days) — Your custom reminder schedule needs to be a list of positive whole numbers separated by commas (e.g. `1,3,7,14`), within the supported limit.

**"Revisions can only be completed for ACTIVE topics"** — You're trying to mark a revision complete on a topic that's been archived. Unarchive the topic to continue tracking it.

**"Invalid revisionPlanId"** — The revision plan you selected for this topic doesn't exist or is no longer available. Reopen the schedule settings and pick a plan from the current list.

**"Revision plan not found"** — The revision plan attached to this topic couldn't be loaded. Refresh the page, and if it persists, re-select a scheduling option for the topic.

**"Revision not found"** — The specific revision you tried to open or complete doesn't exist, or doesn't belong to your account. This usually means it was already completed or the topic was archived — refresh your revision list to see the current state.

## AI Study Plans

**Plan stuck on "generating"** — Generation happens in the background and takes a little time. If it's been unusually long, try refreshing — if it still hasn't resolved, the request may have failed and you can try submitting it again.

**"Too many requests" when creating a plan** — Study plan generation is rate-limited per account to keep the service fair and sustainable for everyone. If you hit this, wait for the cooldown shown before trying again.

**Plan not found when checking status** — Either the plan doesn't exist or doesn't belong to your account. Double-check you're looking at the right one.

**Can't mark a subtopic done yet** — This happens if the plan hasn't finished generating. Wait until its status shows complete before interacting with individual subtopics.

**A checkbox tap doesn't seem to register** — Try again — ProStriver's completion tracking is built to safely ignore duplicate taps rather than double-count them, so an occasional retry is completely safe.

## Challenges

**"An ACTIVE challenge already exists. Quit it before selecting a new one."** — You can only run one challenge at a time. End your current one before starting a different one.

**"No ACTIVE challenge found"** — You tried to interact with a challenge, but you don't currently have one running.

## General

**"Malformed or unreadable request body"** — This typically only comes up if you're interacting with the API directly rather than through the app itself — it means the data sent wasn't valid.

**Unexpected error / something went wrong** — If you see a generic error message, please use the in-app **Feedback** form or email **support@prostriver.me** with what you were doing when it happened — that context helps us track it down quickly.

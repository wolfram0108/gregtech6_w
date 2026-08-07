# Security policy

## Supported versions

This project is in alpha. Only the most recent published release receives fixes; older alpha builds
are not maintained.

## What counts as a security issue here

This is a Minecraft mod, so "security" means something narrower than in a web service. Please report
privately:

- a way to crash or hang a **dedicated server** from an ordinary client;
- a way to run arbitrary code, read files, or make network requests through the mod;
- a way for a client to make the server perform actions its player is not allowed to perform, or to
  read data it should not see (other players' inventories, chest contents, server files);
- unbounded resource use that a player can trigger deliberately (memory, disk, CPU).

Ordinary bugs — wrong recipes, wrong rendering, machines misbehaving, crashes you can only cause on
your own single-player world — are **not** security issues. Please open a normal issue for those; it
gets them fixed faster.

## How to report

Use GitHub's [**private vulnerability reporting**](https://github.com/wolfram0108/gregtech6_w/security/advisories/new) so
the details are not public while the issue is open. Please do not open a public issue for the
categories listed above, and please do not post a working exploit publicly before a fix exists.

Include: what happens, how to reproduce it, the mod and NeoForge versions, whether it needs a
dedicated server, and the relevant part of the log.

## What to expect

You will get an acknowledgement that the report was read, an assessment of whether it is a security
issue as defined above, and a fix in a following release if it is. This is a volunteer project with
no staffed response team, so please allow reasonable time.

# Mixin checks for the Gameoverse 26.1.2 backport

A clean compile doesn't prove the mixins will apply: targets are strings the
compiler never checks, and a failed injection crashes the client at startup.
Run both from the repo root after `./gradlew classes`:

    python3 tools/check_injections.py   # every INVOKE/FIELD/NEW @At target occurs
                                        # inside the injected method's 26.1.2 bytecode
    python3 tools/check_shadows.py      # every @Shadow is declared on the target class
                                        # itself; @Accessor/@Invoker members exist

Both read the game jar from Loom's cache and real JEI from `libs/`. Known false
alarms: constructors of generic classes (check those by hand with `javap -c`).
The injection check caught the PauseScreenConfigButtonMixin crash (26.2's pause
menu icon row doesn't exist in 26.1.2) when self-tested against it.

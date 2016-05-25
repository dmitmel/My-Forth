: pow2
    dup * ;

:pow3
    dup dup * * ;

"4 ^ 2 ^ 3 =" .
4 pow2 pow3 . cr

cr
"=============== QUINE ===============" . cr
quine

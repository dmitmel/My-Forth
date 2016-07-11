: pow_n     \ Usage:   num pow pow_n
            \ Example: 4 2 pow_n => 16
    dup store_var     \ Storing pow to variable
    \ 4 2 [ ] => 4 2 2 [ ] => 4 2 [2]
    2 do dup loop
    \ 4 2 [2] => 4 2 2 [2] => 4 [2] => 4 4 [2]
    get_var 2
    \ 4 4 [2] => 4 4 2 [2] => 4 4 2 2 [2]
    do * loop
    \ 4 4 2 2 [2] => 4 4 [2] => 16 [2]
    ;

2 3 pow_n .

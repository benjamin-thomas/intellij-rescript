let rec f = x => g(x)
@inline
and g = x => f(x)

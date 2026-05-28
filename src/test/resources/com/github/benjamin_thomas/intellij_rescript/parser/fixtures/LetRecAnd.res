let rec isEven = n => n == 0 ? true : isOdd(n - 1)
and isOdd = n => n == 0 ? false : isEven(n - 1)

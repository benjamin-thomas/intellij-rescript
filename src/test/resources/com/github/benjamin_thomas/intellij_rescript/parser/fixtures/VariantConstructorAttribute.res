@unboxed
type responseFormat =
  | Auto(bool)
  | @as("json") Json
  | @as("text") Text

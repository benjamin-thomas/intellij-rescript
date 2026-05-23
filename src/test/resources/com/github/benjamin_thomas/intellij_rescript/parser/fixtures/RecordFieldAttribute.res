type action = {
  @as("type") kind: string,
}

type apiUser = private {
  id: string,
  @as("display_name")
  displayName: string,
}

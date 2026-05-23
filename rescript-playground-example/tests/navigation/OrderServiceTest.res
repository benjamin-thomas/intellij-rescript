// Mirror of src/navigation/OrderService.res.

let approxEq = (a: float, b: float) => Math.abs(a -. b) < 0.0001

let assertApprox = (label, actual, expected) =>
  if approxEq(actual, expected) {
    Console.log(`OK   ${label}`)
  } else {
    Console.log(
      `FAIL ${label}: got ${actual->Float.toString}, expected ${expected->Float.toString}`,
    )
  }

let test_total = () => {
  let lines: array<OrderService.line> = [
    {sku: "A", qty: 2, unitPrice: 10.0},
    {sku: "B", qty: 1, unitPrice: 5.5},
  ]
  assertApprox("total", OrderService.total(lines), 25.5)
}

let test_applyDiscount = () => {
  assertApprox("0% discount", OrderService.applyDiscount(100.0, 0.0), 100.0)
  assertApprox("25% discount", OrderService.applyDiscount(100.0, 25.0), 75.0)
  assertApprox("100% discount", OrderService.applyDiscount(100.0, 100.0), 0.0)
}

let run = () => {
  test_total()
  test_applyDiscount()
}

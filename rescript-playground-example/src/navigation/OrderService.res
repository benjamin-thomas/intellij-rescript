// Paired with tests/navigation/OrderServiceTest.res. Function names here
// (`total`, `applyDiscount`) align with `test_total`, `test_applyDiscount` so
// that function-level Go-to-Test (ticket navigation/020) has a clean target.

type line = {sku: string, qty: int, unitPrice: float}

let total = (lines: array<line>) =>
  lines->Array.reduce(0.0, (acc, l) => acc +. Int.toFloat(l.qty) *. l.unitPrice)

let applyDiscount = (subtotal, pct) =>
  if pct <= 0.0 {
    subtotal
  } else if pct >= 100.0 {
    0.0
  } else {
    subtotal *. (1.0 -. pct /. 100.0)
  }

let summary = (lines, discountPct) => {
  let sub = total(lines)
  let final = applyDiscount(sub, discountPct)
  (sub, final)
}

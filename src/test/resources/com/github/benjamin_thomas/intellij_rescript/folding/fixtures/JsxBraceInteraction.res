let card =
  <fold text='<Card ...>'><Card
    header=<fold text='{...}'>{
      <Button label="Edit" />
    }</fold>
  /></fold>

let panel =
  <fold text='<section ...>'><section>
    <fold text='{...}'>{
      React.string("head")
    }</fold>
  </section></fold>

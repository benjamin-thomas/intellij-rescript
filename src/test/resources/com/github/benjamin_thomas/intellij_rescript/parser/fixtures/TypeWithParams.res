type state<'model, 'effect> = {
  model: 'model,
  effectsQueue: array<'effect>,
}

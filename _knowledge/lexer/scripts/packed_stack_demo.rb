#!/usr/bin/env ruby

require "io/console"

# Interactive packed-stack demo for the ReScript lexer context stack (v2).
#
# From the repo root, run:
#
#   ruby _knowledge/lexer/scripts/packed_stack_demo.rb
#
# Then press a key. You do not need Enter when running in a terminal:
#
#   +  push a TEMPLATE frame, like reading `${` inside a backtick template
#   a  push a JSX_ATTR frame, like reading `{` inside an opening tag
#   c  push a JSX_CONTENT frame, like reading the `>` that opens children
#   n  bump the content frame's child count, like a nested unbraced `<x>`
#   {  increment brace depth inside the current frame
#   }  decrement brace depth, popping TEMPLATE/JSX_ATTR frames at zero
#   -  close: content frames lose one child (pop at zero), others act like `}`
#   r  reset
#   Ctrl-L clear the screen and reprint state
#   q  quit (Ctrl-D or Ctrl-C also quit)
#
# Mental model:
#
# - IntelliJ gives the lexer only one integer to save restart state.
# - Bits 8..31 of that integer are a tiny fixed-size stack: three 8-bit frames.
# - Frame byte = 2-bit kind (high) + 6-bit payload.
#     TEMPLATE (0):    2-bit return-state selector + 4-bit brace depth
#     JSX_ATTR (1):    6-bit brace depth
#     JSX_CONTENT (2): 3-bit child count + 3-bit brace depth
# - The rightmost frame is the innermost open context.
# - A live frame's byte is never zero (depth or count >= 1), so a 0x00 byte
#   unambiguously means "empty slot".
# - The "source" line below is a toy string illustrating lexer events, not a
#   complete ReScript program at every intermediate step.

FRAME_BITS = 8
FRAME_MASK = 0xFF
CONTEXT_BITS = 24
CONTEXT_MASK = (1 << CONTEXT_BITS) - 1

KIND_SHIFT = 6
KIND_TEMPLATE = 0
KIND_JSX_ATTR = 1
KIND_JSX_CONTENT = 2

TEMPLATE_DEPTH_MASK = 0xF
ATTR_DEPTH_MASK = 0x3F
CONTENT_DEPTH_MASK = 0x7
CONTENT_COUNT_SHIFT = 3
CONTENT_COUNT_MASK = 0x7

def groups(context)
  context
    .to_s(2)
    .rjust(CONTEXT_BITS, "0")
    .scan(/.{8}/)
end

def frame_desc(byte)
  case byte >> KIND_SHIFT
  when KIND_TEMPLATE
    "TEMPLATE(sel=#{(byte >> 4) & 0x3}, depth=#{byte & TEMPLATE_DEPTH_MASK})"
  when KIND_JSX_ATTR
    "JSX_ATTR(depth=#{byte & ATTR_DEPTH_MASK})"
  when KIND_JSX_CONTENT
    "JSX_CONTENT(count=#{(byte >> CONTENT_COUNT_SHIFT) & CONTENT_COUNT_MASK}, depth=#{byte & CONTENT_DEPTH_MASK})"
  else
    "?(#{byte})"
  end
end

def frames(context)
  groups(context).map { |bits| bits.to_i(2) }.drop_while(&:zero?)
end

def show(context, source, label = nil)
  prefix = label ? "#{label}: " : ""
  descs = frames(context).map { |b| frame_desc(b) }
  puts "#{prefix}int=#{context}   binary=#{groups(context).join(" ")}"
  puts "  frames(outer->inner)=#{descs.inspect}"
  puts "  source=#{source.inspect}"
  context
end

def top_kind(context)
  (context & FRAME_MASK) >> KIND_SHIFT
end

def top_byte(context)
  context & FRAME_MASK
end

def depth_mask(context)
  case top_kind(context)
  when KIND_TEMPLATE then TEMPLATE_DEPTH_MASK
  when KIND_JSX_CONTENT then CONTENT_DEPTH_MASK
  else ATTR_DEPTH_MASK
  end
end

def current_depth(context)
  context & depth_mask(context)
end

def push_frame(context, kind, payload)
  ((context << FRAME_BITS) & CONTEXT_MASK) | (kind << KIND_SHIFT) | payload
end

def increment_depth(context)
  raise "no active frame" if top_byte(context).zero?
  raise "current frame's depth is full (saturates in the real lexer)" if current_depth(context) == depth_mask(context)

  context + 1
end

def decrement_depth(context)
  raise "no active frame" if top_byte(context).zero?
  raise "depth already zero" if current_depth(context).zero?

  context - 1
end

def pop_frame(context)
  context >> FRAME_BITS
end

def close_brace(context)
  context = decrement_depth(context)
  if current_depth(context).zero? && top_kind(context) != KIND_JSX_CONTENT
    pop_frame(context)
  else
    context
  end
end

def close_child(context)
  if top_kind(context) == KIND_JSX_CONTENT
    count = (top_byte(context) >> CONTENT_COUNT_SHIFT) & CONTENT_COUNT_MASK
    raise "no open child element" if count.zero?

    context -= (1 << CONTENT_COUNT_SHIFT)
    count == 1 ? pop_frame(context) : context
  else
    close_brace(context)
  end
end

def print_interactive_help
  puts
  puts "Keys:"
  puts "  +  push TEMPLATE frame: enter `${`"
  puts "  a  push JSX_ATTR frame: `{` in an opening tag"
  puts "  c  push JSX_CONTENT frame: `>` opens children"
  puts "  n  nested unbraced element: content count += 1"
  puts "  {  increment brace depth inside current frame"
  puts "  }  decrement brace depth (TEMPLATE/JSX_ATTR pop at zero)"
  puts "  -  close: content child count -= 1 (pop at zero), else like `}`"
  puts "  r  reset"
  puts "  Ctrl-L clear screen"
  puts "  q  quit (Ctrl-D / Ctrl-C too)"
  puts
end

def read_key
  $stdin.tty? ? $stdin.getch : $stdin.read(1)
end

def interactive
  ctx = 0
  source = ""
  print_interactive_help
  show(ctx, source, "initial")

  loop do
    print "key> "
    input = read_key
    puts if $stdin.tty?

    break if input.nil? || input.empty?

    begin
      case input
      when "+"
        source << "${"
        ctx = push_frame(ctx, KIND_TEMPLATE, 1)
        show(ctx, source, "push +  read `${`")
      when "a"
        source << "={"
        ctx = push_frame(ctx, KIND_JSX_ATTR, 1)
        show(ctx, source, "push a  read `={`")
      when "c"
        source << "<x>"
        ctx = push_frame(ctx, KIND_JSX_CONTENT, 1 << CONTENT_COUNT_SHIFT)
        show(ctx, source, "push c  read `<x>`")
      when "n"
        source << "<y>"
        raise "top frame is not JSX_CONTENT" unless top_kind(ctx) == KIND_JSX_CONTENT

        ctx += (1 << CONTENT_COUNT_SHIFT)
        show(ctx, source, "nest n  read `<y>`")
      when "{"
        source << "{"
        ctx = increment_depth(ctx)
        show(ctx, source, "brace { read `{`")
      when "}"
        source << "}"
        ctx = close_brace(ctx)
        show(ctx, source, "brace } read `}`")
      when "-"
        source << (top_kind(ctx) == KIND_JSX_CONTENT ? "</x>" : "}")
        ctx = close_child(ctx)
        show(ctx, source, "close -")
      when "r"
        ctx = 0
        source = ""
        show(ctx, source, "reset")
      when "\f"
        system("clear")
        print_interactive_help
        show(ctx, source, "current")
      when "q", "\u0003", "\u0004"
        break
      when "h", "?"
        print_interactive_help
      when "\n", "\r"
        next
      else
        puts "unknown command: #{input.inspect}"
      end
    rescue StandardError => e
      puts "error: #{e.message}"
    end
  end

  nil
rescue Interrupt
  puts
  nil
end

interactive if __FILE__ == $PROGRAM_NAME

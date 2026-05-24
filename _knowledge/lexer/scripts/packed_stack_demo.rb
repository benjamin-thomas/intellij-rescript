#!/usr/bin/env ruby

require "io/console"

# Interactive packed-stack demo for the ReScript lexer interpolation state.
#
# From the repo root, run:
#
#   ruby _knowledge/lexer/scripts/packed_stack_demo.rb
#
# Then press a key. You do not need Enter when running in a terminal:
#
#   +  enter a nested template interpolation frame, like reading `${`
#   -  close the current interpolation frame, like reading the matching `}`
#   {  increment brace depth inside the current interpolation
#   }  decrement brace depth, popping if the current frame reaches zero
#   r  reset
#   Ctrl-L clear the screen and reprint state
#   q  quit
#   Ctrl-D or Ctrl-C also quit
#
# Mental model:
#
# - IntelliJ gives the lexer only one integer to save restart state.
# - We use part of that integer as a tiny fixed-size stack.
# - Each stack "frame" is 5 bits wide.
# - 5 bits can store values 0..31.
# - The rightmost frame is the current interpolation depth.
# - Higher/left frames remember outer template interpolations.
# - The "source" line below is a toy string showing which characters each key
#   appends; it is meant to illustrate lexer events, not produce a complete
#   ReScript program at every intermediate step.

FRAME_BITS = 5
FRAME_MASK = 0b11111
CONTEXT_BITS = 20
CONTEXT_MASK = (1 << CONTEXT_BITS) - 1

def groups(context)
  context
    .to_s(2)
    .rjust(CONTEXT_BITS, "0")
    .scan(/.{5}/)
end

def frames(context)
  groups(context).map { |bits| bits.to_i(2) }.drop_while(&:zero?)
end

def show(context, source, label = nil)
  prefix = label ? "#{label}: " : ""
  puts "#{prefix}int=#{context}   binary=#{groups(context).join(" ")}   frames=#{frames(context).inspect}"
  puts "  source=#{source.inspect}"
  context
end

def current_depth(context)
  context & FRAME_MASK
end

def push_frame(context, depth = 1)
  unless (0..FRAME_MASK).cover?(depth)
    raise ArgumentError, "depth must fit in #{FRAME_BITS} bits: 0..#{FRAME_MASK}"
  end

  ((context << FRAME_BITS) & CONTEXT_MASK) | depth
end

def increment_depth(context)
  depth = current_depth(context)
  raise "no active frame" if depth.zero?
  raise "current frame is full" if depth == FRAME_MASK

  context + 1
end

def decrement_depth(context)
  depth = current_depth(context)
  raise "no active frame" if depth.zero?

  context - 1
end

def pop_frame(context)
  context >> FRAME_BITS
end

def close_brace(context)
  context = decrement_depth(context)
  current_depth(context).zero? ? pop_frame(context) : context
end

def print_interactive_help
  puts
  puts "Keys:"
  puts "  +  push frame: enter nested `${`"
  puts "  -  close/pop current interpolation"
  puts "  {  increment brace depth inside current frame"
  puts "  }  decrement brace depth, popping at zero"
  puts "  r  reset"
  puts "  Ctrl-L clear screen"
  puts "  q  quit"
  puts "  Ctrl-D / Ctrl-C quit"
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

    command = input

    begin
      case command
      when "+"
        source << "${"
        ctx = push_frame(ctx, 1)
        show(ctx, source, "push +  read `${`")
      when "-"
        source << "}"
        ctx = close_brace(ctx)
        show(ctx, source, "close - read `}`")
      when "{"
        source << "{"
        ctx = increment_depth(ctx)
        show(ctx, source, "brace { read `{`")
      when "}"
        source << "}"
        ctx = close_brace(ctx)
        show(ctx, source, "brace } read `}`")
      when "r"
        ctx = 0
        source = ""
        show(ctx, source, "reset")
      when "\f"
        system("clear")
        print_interactive_help
        show(ctx, source, "current")
      when "q"
        break
      when "\u0003", "\u0004"
        break
      when "h", "?"
        print_interactive_help
      when "\n", "\r"
        next
      else
        puts "unknown command: #{command.inspect}"
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

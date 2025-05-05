# Minecraft Robot Plugin

## Overview
The Minecraft Robot Plugin allows players to control a special robot block using a wide range of programmable commands. Players can move, rotate, trace, and automate their robot using simple or advanced instructions.

## Features
- Each player receives a special robot block (magenta glazed terracotta) on join.
- Only one robot per player (placing a new one destroys the previous).
- Move the robot forward, backward, up, and down.
- Rotate the robot by 45° or 90° increments.
- Draw colored wool traces as the robot moves (`/trace on <color>`).
- Program complex sequences with `/repete` (repeat) and stop them with `/robot stop`.
- All players can use all commands (no OP required).
- Robot name tag above the block.
- Robot and trace are cleaned up on disconnect or block break.
- Inventory is cleared on disconnect.

## Installation
1. Download the plugin JAR file.
2. Place the JAR file in the `plugins` directory of your Minecraft server.
3. Restart the server to load the plugin.

## Usage
When a player joins, they receive a magenta glazed terracotta block. Place it to spawn your robot. Use the following commands to control it:

### Main Commands
- `/avance <nombre>`: Move the robot forward by `<nombre>` blocks.
- `/recule <nombre>`: Move the robot backward by `<nombre>` blocks.
- `/tourne <degrés>`: Rotate the robot by `<degrés>` (45 or 90 recommended).
- `/monte <nombre>`: Move the robot up by `<nombre>` blocks.
- `/descends <nombre>`: Move the robot down by `<nombre>` blocks (not below Y=-60).
- `/trace on <couleur>`: Enable trace mode with the specified wool color (e.g. bleu, rouge, vert, etc.).
- `/trace off`: Disable trace mode.
- `/repete <nb> (<instructions>)`: Repeat a sequence of instructions `<nb>` times. Example: `/repete 4 (avance 5 tourne 90)`
- `/robot stop`: Instantly stop any running `/repete` sequence for your robot.

### Example Sequences
- `/repete 4 (trace on bleu avance 5 tourne 90)` — Draws a blue square.
- `/repete 8 (trace on rouge avance 3 tourne 45)` — Draws a red octagon.
- `/repete 10 (monte 1 avance 2 descends 1 recule 2)` — Draws a 3D zigzag.

## Configuration
The plugin is configured via the `plugin.yml` file in `src/main/resources`.

## Contributing
Contributions are welcome! Please feel free to submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.
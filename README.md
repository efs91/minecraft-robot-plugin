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
- Create and reuse custom functions with parameters using the `/fonction` command.
- Support for nested repeats and function calls for advanced programming.
- All players can use all commands (no OP required).
- Robot name tag above the block.
- Robot and trace are cleaned up on disconnect or block break.
- Inventory is cleared on disconnect.
- Functions are saved between sessions for each player.

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
- `/fonction create <nom>(<params>) <instructions>`: Define custom functions with parameters.
- `/fonction read <nom>(<args>)`: Execute a custom function with arguments.
- `/fonction delete <nom>`: Delete a previously defined custom function.
- `/robot stop`: Instantly stop any running `/repete` sequence for your robot.

### Example Sequences
- `/repete 4 (trace on bleu avance 5 tourne 90)` — Draws a blue square.
- `/repete 8 (trace on rouge avance 3 tourne 45)` — Draws a red octagon.
- `/repete 10 (monte 1 avance 2 descends 1 recule 2)` — Draws a 3D zigzag.
- `/repete 2 (repete 2 (avance 10 tourne 90))` — Uses nested repeats to create complex patterns.

### Using Custom Functions with Parameters
The `/fonction` command now supports parameters for more flexible programming:

1. Creating a function with parameters: `/fonction create carre(taille) repete 4 (avance taille tourne 90)`
2. Using the function with arguments: `/fonction read carre(10)` or simply `read carre(10)` inside other functions
3. Creating more complex functions: `/fonction create rectangle(largeur,hauteur) repete 2 (avance largeur tourne 90 avance hauteur tourne 90)`
4. Nested function calls: `/fonction create maison(taille) read rectangle(taille,taille); avance taille; tourne 90; read triangle(taille)`
5. View all your functions: just type `/fonction` without arguments
6. Delete a function: `/fonction delete carre` to remove the `carre` function

### Advanced Programming Techniques
- **Nested Repeats**: You can now use repeats inside repeats:
  ```
  /repete 2 (repete 2 (avance 10 tourne 90))
  ```
  
- **Function Calls in Repeats**: Call functions inside repeat loops:
  ```
  /repete 4 (fonction read carre(10) tourne 90)
  ```
  
- **Parameterized Functions**: Create reusable functions with variable inputs:
  ```
  /fonction create spiral(tours,pas) repete tours (avance pas tourne 90 avance pas tourne 90 avance pas*2 tourne 90 avance pas*2)
  ```

Functions are saved for each player and persist between server sessions.

## Configuration
The plugin is configured via the `plugin.yml` file in `src/main/resources`.

## Contributing
Contributions are welcome! Please feel free to submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the GNU General Public License v3 (GPL-3.0). See the LICENSE file for more details.
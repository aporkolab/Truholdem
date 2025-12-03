# Truholdem v.1.3.0

[![Build](https://img.shields.io/github/actions/workflow/status/APorkolab/Truholdem/ci.yml?branch=main)](../../actions)
[![License](https://img.shields.io/badge/license-MIT-informational.svg)](LICENSE)
[![Issues](https://img.shields.io/github/issues/APorkolab/Truholdem.svg)](../../issues)


Truholdem is a sophisticated poker program designed to offer a comprehensive platform for learning, practicing, and simulating Texas Hold'em poker games. This README provides an in-depth overview of the project, installation instructions, usage guidelines, and contribution details.

## Project Overview

Truholdem is developed to simulate Texas Hold'em poker, providing a rich environment for users to enhance their poker skills, test strategies, and enjoy engaging poker sessions. The project features a robust backend in Java using Spring Boot and an interactive frontend using Angular and SCSS.

The main goal is to imitate the world of Win95/Win98 poker simulators and provide an overwhelmingly positive experience for users.

### Features

- **Game Simulation**: Fully functional Texas Hold'em poker simulation, allowing users to experience realistic gameplay.
- **User Interface**: Intuitive and responsive interface designed with modern web technologies.
- **Multiplayer Support**: Capability for multiple players to join and play poker games, including AI opponents.
- **AI Opponents**: Play against AI opponents with basic decision-making capabilities.
- **Customizable Settings**: Adjust game rules and settings to create personalized poker experiences.
- **Cross-Platform Compatibility**: Accessible on various devices and platforms, ensuring a seamless experience.
- **Betting System**: Implemented betting mechanics including raising, calling, and folding.
- **Hand Evaluation**: Robust hand evaluation system to determine the winner of each round.
- **Game Phases**: Proper implementation of all poker game phases (Pre-flop, Flop, Turn, River, Showdown).

## Installation

To get started with Truholdem, follow these steps:

### Prerequisites

- Java Development Kit (JDK) 21 or later
- Node.js and npm
- Maven for building the backend project

### Steps

1. Clone the repository:
   ```
   git clone https://github.com/APorkolab/Truholdem.git
   ```

2. Navigate to the project directory:
   ```
   cd Truholdem
   ```

3. Install backend dependencies and build the project:
   ```
   cd backend
   ./mvnw clean install
   ```

4. Install frontend dependencies:
   ```
   cd ../frontend
   npm install
   ```

5. Start the backend server:
   ```
   cd ../backend
   ./mvnw spring-boot:run
   ```

6. Start the frontend development server:
   ```
   cd ../frontend
   npm start
   ```

## Usage

Once the servers are running, access the application in your web browser at `http://localhost:4200`. The backend runs at `http://localhost:4200`. Use the interface to play simulated Texas Hold'em poker games, practice strategies, and enjoy learning poker.

## API Documentation

The backend API is documented using Swagger UI. You can access the API documentation at `http://localhost:8080/swagger-ui.html` when the backend server is running.

## Testing

The project includes a comprehensive test suite. To run the tests, use the following command in the backend directory:

```
./mvnw test
```

## Contributing

Contributions are welcome! If you'd like to contribute to Truholdem, please follow these guidelines:

1. Fork the repository.
    
2. Create a new branch for your feature or bugfix: 

	     git checkout -b feature-name
    
3. Commit your changes: +++ 

		git commit -m "Description of feature or fix"
    
4. Push to the branch:

		git push origin feature-name
    
5. Open a pull request with a description of your changes.
    

## License

This project is licensed under the MIT License.

## Author

Truholdem is developed and maintained by [Adam Dr. Porkolab](https://github.com/APorkolab). The another projects of the author may access on [www.aporkolab.com](https://www.aporkolab.com). For any questions or inquiries, please open an issue on the repository or contact the author directly.

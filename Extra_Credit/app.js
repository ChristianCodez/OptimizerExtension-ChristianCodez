document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('fetchBtn').addEventListener('click', async () => {
    const city = document.getElementById('cityInput').value;
    const useFahrenheit = document.getElementById('unitToggle').checked;
    const units = useFahrenheit ? 'imperial' : 'metric';
    const unitSymbol = useFahrenheit ? '°F' : '°C';
    const apiKey = '5fde1ec2900ff8ad6c9c9f28b0b7d060'; // Replace with your real API key
    const url = `https://api.openweathermap.org/data/2.5/weather?q=${city}&appid=${apiKey}&units=${units}`;

    try {
      const response = await axios.get(url);
      const data = response.data;

      // Determine the weather condition
      const condition = data.weather[0].main.toLowerCase();

      // Remove any previous background class
      document.body.className = '';

      // Add new background class based on the condition
      if (condition.includes('cloud')) {
        document.body.classList.add('cloudy');
      } else if (condition.includes('rain') || condition.includes('drizzle') || condition.includes('thunderstorm')) {
        document.body.classList.add('rainy');
      } else if (condition.includes('snow')) {
        document.body.classList.add('snow');
      } else if (condition.includes('clear')) {
        document.body.classList.add('clear');
      } else {
        document.body.classList.add('sunny');
      }

      // Display the weather data
      const timezoneOffset = data.timezone;
      const utc = new Date().getTime() + new Date().getTimezoneOffset() * 60000;
      const localDate = new Date(utc + timezoneOffset * 1000);
      const localTime = localDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      const output = `
        <h2>Weather in ${data.name}</h2>
        <p>Temperature: ${data.main.temp}${unitSymbol}</p>
        <p>Humidity: ${data.main.humidity}%</p>
        <p>Conditions: ${data.weather[0].description}</p>
        <p>Local Time: ${localTime}</p>
      `;
      document.getElementById('weatherOutput').innerHTML = output;

      // Map setup
      const lat = data.coord.lat;  // Latitude from API response
      const lon = data.coord.lon;  // Longitude from API response
      console.log('Map coordinates:', lat, lon);  // Log the coordinates to check

      // Wait for the map container to be available
      const mapContainer = document.getElementById('map');
      if (mapContainer) {
        const map = L.map('map').setView([lat, lon], 13); // Initialize map at city coordinates
        console.log('Map initialized');  // Log to verify map initialization

        // Add OpenStreetMap tiles
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        // Add marker at the city location
        L.marker([lat, lon]).addTo(map)
          .bindPopup(`<b>${data.name}</b><br>Weather: ${data.weather[0].description}`)
          .openPopup();
      } else {
        console.error('Map container not found');
      }
    } catch (error) {
      document.getElementById('weatherOutput').innerHTML = '<p>City not found or error fetching data.</p>';
      console.error(error);
    }
  });
});

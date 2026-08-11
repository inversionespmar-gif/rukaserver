import { resolveBestLeague } from './src/resolver/bestleague.js';
const mpd = await resolveBestLeague('https://telelibrefull.online/en-vivo/warner/embed.php');
console.log(mpd);

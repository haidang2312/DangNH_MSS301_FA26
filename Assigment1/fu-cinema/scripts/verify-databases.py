"""Verify database ownership, Unicode, persistence types, and reservation integrity."""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
checks = []


def run(*args):
    result = subprocess.run(['docker', 'exec', *args], check=True, capture_output=True)
    return result.stdout.decode('utf-8').strip()


def check(name, condition, evidence):
    checks.append({'check': name, 'passed': bool(condition), 'evidence': evidence})
    print(('PASS ' if condition else 'FAIL ') + name, flush=True)
    assert condition, name


sql = run('cinema-sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'localhost',
          '-U', 'sa', '-P', 'Fucinema@2026', '-C', '-d', 'cinema_customer',
          '-h', '-1', '-W', '-Q', "SET NOCOUNT ON; SELECT customer_name FROM customer WHERE email='an@gmail.com'; SELECT COUNT(*) FROM flyway_schema_history;")
check('SQL Server preserves Vietnamese profile name', 'Nguyễn Văn An (updated)' in sql, sql.splitlines())
check('SQL Server Flyway applied schema and seed migrations', sql.splitlines()[-1].strip() == '2', sql.splitlines()[-1].strip())

mongo_script = '''JSON.stringify({
  counts: {genres:db.genres.countDocuments(),rooms:db.cinema_rooms.countDocuments(),movies:db.movies.countDocuments(),showtimes:db.showtimes.countDocuments()},
  showtimes:db.showtimes.countDocuments(),
  decimalPrices:db.showtimes.countDocuments({ticketPrice:{$type:'decimal'}}),
  objectIds:db.showtimes.countDocuments({_id:{$type:'objectId'}}),
  stringMovieIds:db.showtimes.countDocuments({movieId:{$type:'string'}}),
  uniqueGenreIndex:db.genres.getIndexes().some(i=>i.unique&&i.key.genreName===1),
  uniqueRoomIndex:db.cinema_rooms.getIndexes().some(i=>i.unique&&i.key.roomName===1),
  roomStartIndex:db.showtimes.getIndexes().some(i=>i.key.roomId===1&&i.key.startTime===1)
})'''
mongo = json.loads(run('cinema-mongo', 'mongosh', '--quiet', '-u', 'root', '-p', 'password',
                       '--authenticationDatabase', 'admin', 'cinema_movie', '--eval', mongo_script))
check('MongoDB has all four catalog collections', all(v > 0 for v in mongo['counts'].values()), mongo['counts'])
check('Showtime prices are Decimal128', mongo['showtimes'] == mongo['decimalPrices'], mongo['decimalPrices'])
check('Showtime IDs are ObjectId and movie references are strings', mongo['showtimes'] == mongo['objectIds'] == mongo['stringMovieIds'], mongo['objectIds'])
check('MongoDB unique and compound indexes exist', mongo['uniqueGenreIndex'] and mongo['uniqueRoomIndex'] and mongo['roomStartIndex'], {k: v for k, v in mongo.items() if k.endswith('Index')})

mysql_sql = '''SELECT COUNT(*) FROM flyway_schema_history;
SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns WHERE table_schema='cinema_booking' AND table_name='booking_detail' AND column_name='showtime_id';
SELECT COUNT(*) FROM booking_detail WHERE movie_title='Hành Trình Phương Nam';
SELECT COUNT(*) FROM seat_reservation r JOIN booking b ON r.booking_id=b.booking_id WHERE b.booking_status<>'CONFIRMED';
SELECT (SELECT COUNT(*) FROM seat_reservation)=(SELECT COUNT(*) FROM booking_detail d JOIN booking b ON d.booking_id=b.booking_id WHERE b.booking_status='CONFIRMED');'''
values = run('cinema-mysql', 'mysql', '-uroot', '-pmysql', '--default-character-set=utf8mb4',
             '-N', '-B', 'cinema_booking', '-e', mysql_sql).splitlines()
check('MySQL Flyway migration applied', values[0] == '1', values[0])
check('MySQL showtime reference is VARCHAR(24)', values[1] == '24', values[1])
check('MySQL stores movie title snapshots', int(values[2]) > 0, int(values[2]))
check('No cancelled booking retains an active seat reservation', values[3] == '0', values[3])
check('Every confirmed ticket has exactly one active reservation', values[4] == '1', values[4])

(ROOT / '.runtime/database-verification.json').write_text(json.dumps(checks, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'{len(checks)} database checks passed.', flush=True)

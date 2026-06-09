from datetime import datetime, timezone

from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


def _now_ms() -> int:
    return int(datetime.now(timezone.utc).timestamp() * 1000)


class Route(db.Model):
    __tablename__ = "route"

    id = db.Column(db.Integer, primary_key=True)
    client_id = db.Column(db.Integer, nullable=False, unique=True, index=True)
    name = db.Column(db.String(255), nullable=False)
    start_ts = db.Column(db.BigInteger, nullable=False)
    end_ts = db.Column(db.BigInteger, nullable=True)
    created_at = db.Column(db.BigInteger, nullable=False, default=_now_ms)

    points = db.relationship("TrackPoint", backref="route", cascade="all, delete-orphan")
    notes = db.relationship("Note", backref="route", cascade="all, delete-orphan")

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "client_id": self.client_id,
            "name": self.name,
            "start_ts": self.start_ts,
            "end_ts": self.end_ts,
            "point_count": len(self.points),
            "note_count": len(self.notes),
        }


class TrackPoint(db.Model):
    __tablename__ = "track_point"
    __table_args__ = (
        db.UniqueConstraint("route_id", "client_id", name="uq_point_route_client"),
    )

    id = db.Column(db.Integer, primary_key=True)
    route_id = db.Column(db.Integer, db.ForeignKey("route.id", ondelete="CASCADE"), nullable=False, index=True)
    client_id = db.Column(db.Integer, nullable=False)
    lat = db.Column(db.Float, nullable=False)
    lon = db.Column(db.Float, nullable=False)
    ts = db.Column(db.BigInteger, nullable=False)
    accuracy_m = db.Column(db.Float, nullable=False, default=0.0)


class Note(db.Model):
    __tablename__ = "note"
    __table_args__ = (
        db.UniqueConstraint("route_id", "client_id", name="uq_note_route_client"),
    )

    id = db.Column(db.Integer, primary_key=True)
    route_id = db.Column(db.Integer, db.ForeignKey("route.id", ondelete="CASCADE"), nullable=False, index=True)
    client_id = db.Column(db.Integer, nullable=False)
    lat = db.Column(db.Float, nullable=False)
    lon = db.Column(db.Float, nullable=False)
    ts = db.Column(db.BigInteger, nullable=False)
    text = db.Column(db.Text, nullable=True)
    voice_path = db.Column(db.String(512), nullable=True)

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "client_id": self.client_id,
            "route_id": self.route_id,
            "lat": self.lat,
            "lon": self.lon,
            "ts": self.ts,
            "text": self.text,
            "has_voice": self.voice_path is not None,
        }

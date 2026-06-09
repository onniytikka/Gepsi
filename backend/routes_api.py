import json
import os
import uuid
from pathlib import Path

from flask import Blueprint, current_app, jsonify, request, send_file
from werkzeug.utils import secure_filename

from models import Note, Route, TrackPoint, db

bp = Blueprint("api", __name__, url_prefix="/api")


@bp.get("/health")
def health():
    return jsonify(status="ok"), 200


@bp.post("/routes")
def create_route():
    data = request.get_json(force=True)
    client_id = data.get("client_id")
    if client_id is None:
        return jsonify(error="client_id required"), 400

    existing = Route.query.filter_by(client_id=client_id).first()
    if existing:
        existing.name = data.get("name", existing.name)
        existing.end_ts = data.get("end_ts", existing.end_ts)
        db.session.commit()
        return jsonify(existing.to_dict()), 200

    route = Route(
        client_id=client_id,
        name=data.get("name", f"Route {client_id}"),
        start_ts=data["start_ts"],
        end_ts=data.get("end_ts"),
    )
    db.session.add(route)
    db.session.commit()
    return jsonify(route.to_dict()), 201


@bp.get("/routes")
def list_routes():
    routes = Route.query.order_by(Route.start_ts.desc()).all()
    return jsonify([r.to_dict() for r in routes]), 200


@bp.post("/routes/<int:client_id>/points/batch")
def add_points(client_id: int):
    route = Route.query.filter_by(client_id=client_id).first()
    if route is None:
        return jsonify(error="route not found"), 404

    payload = request.get_json(force=True)
    points = payload.get("points", [])
    inserted = 0
    for p in points:
        if TrackPoint.query.filter_by(route_id=route.id, client_id=p["client_id"]).first():
            continue
        db.session.add(TrackPoint(
            route_id=route.id,
            client_id=p["client_id"],
            lat=p["lat"],
            lon=p["lon"],
            ts=p["ts"],
            accuracy_m=p.get("accuracy_m", 0.0),
        ))
        inserted += 1
    db.session.commit()
    return jsonify(inserted=inserted, total=len(points)), 200


@bp.post("/routes/<int:client_id>/notes")
def add_note(client_id: int):
    route = Route.query.filter_by(client_id=client_id).first()
    if route is None:
        return jsonify(error="route not found"), 404

    meta_raw = request.form.get("meta")
    if not meta_raw:
        return jsonify(error="meta field required"), 400
    meta = json.loads(meta_raw)

    note_client_id = meta["client_id"]
    existing = Note.query.filter_by(route_id=route.id, client_id=note_client_id).first()
    if existing:
        return jsonify(existing.to_dict()), 200

    voice_path = None
    if "voice" in request.files:
        f = request.files["voice"]
        uploads = Path(current_app.config["UPLOAD_DIR"]) / str(route.id)
        uploads.mkdir(parents=True, exist_ok=True)
        filename = secure_filename(f"{uuid.uuid4().hex}.m4a")
        dest = uploads / filename
        f.save(dest)
        voice_path = str(dest.relative_to(current_app.config["UPLOAD_DIR"]))

    note = Note(
        route_id=route.id,
        client_id=note_client_id,
        lat=meta["lat"],
        lon=meta["lon"],
        ts=meta["ts"],
        text=meta.get("text"),
        voice_path=voice_path,
    )
    db.session.add(note)
    db.session.commit()
    return jsonify(note.to_dict()), 201


@bp.get("/routes/<int:client_id>/notes/<int:note_client_id>/voice")
def get_voice(client_id: int, note_client_id: int):
    route = Route.query.filter_by(client_id=client_id).first()
    if route is None:
        return jsonify(error="route not found"), 404
    note = Note.query.filter_by(route_id=route.id, client_id=note_client_id).first()
    if note is None or note.voice_path is None:
        return jsonify(error="voice not found"), 404
    abs_path = Path(current_app.config["UPLOAD_DIR"]) / note.voice_path
    return send_file(abs_path, mimetype="audio/mp4")

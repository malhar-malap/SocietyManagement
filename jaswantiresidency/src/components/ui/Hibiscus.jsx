import React from 'react';

/*
 * The society's flower — Jaswand (hibiscus) — drawn as line art so it stays crisp at any size.
 * Five scalloped petals around a centre, with the long stamen column hibiscus is known for.
 */
const PETAL = 'M0,-4 C-11,-12 -29,-25 -30,-45 C-31,-56 -23,-63 -15,-60 C-11,-66 -4,-67 0,-63 C4,-67 11,-66 15,-60 C23,-63 31,-56 30,-45 C29,-25 11,-12 0,-4Z';
const VEINS = 'M0,-10 L0,-50 M0,-18 Q-8,-31 -16,-44 M0,-18 Q8,-31 16,-44';
const ANGLES = [0, 72, 144, 216, 288];
const POLLEN = [[35, -63], [41, -58], [29, -58], [39, -65], [44, -63], [33, -54]];

const Petals = ({ withVeins }) => (
    <>
        {ANGLES.map((a, i) => (
            <g key={a} transform={`rotate(${a})`} className="hb-petal" style={{ '--i': i }}>
                <path d={PETAL} pathLength="1" />
                {withVeins && <path d={VEINS} pathLength="1" className="hb-vein" />}
            </g>
        ))}
    </>
);

const Stamen = () => (
    <g className="hb-stamen">
        <path d="M0,0 C6,-16 20,-38 37,-58" pathLength="1" className="hb-column" />
        {POLLEN.map(([x, y], i) => <circle key={i} cx={x} cy={y} r="2.6" className="hb-pollen" style={{ '--i': i }} />)}
        <circle r="5" className="hb-eye" />
    </g>
);

/** Static brand mark. */
export const HibiscusMark = ({ size = 32, className = '', title }) => (
    <svg
        viewBox="-72 -72 144 144"
        width={size}
        height={size}
        className={`hibiscus-mark ${className}`}
        role={title ? 'img' : undefined}
        aria-hidden={title ? undefined : true}
        aria-label={title}
    >
        <g transform="rotate(-24)">
            <Petals withVeins={size >= 48} />
            <Stamen />
        </g>
    </svg>
);

/** Loading indicator: the petals draw in one by one, bloom, then let go. */
export const FlowerLoader = ({ size = 72, label = 'Loading', showLabel = false, className = '' }) => (
    <div className={`flower-loader ${className}`} role="status" aria-live="polite">
        <svg viewBox="-72 -72 144 144" width={size} height={size} aria-hidden="true">
            <g className="hb-spin">
                <g transform="rotate(-24)">
                    <Petals withVeins />
                    <Stamen />
                </g>
            </g>
        </svg>
        {showLabel ? <span className="flower-loader-label">{label}</span> : <span className="visually-hidden">{label}</span>}
    </div>
);

/** Fills the space it is given (a page body, a card) with a centred loader. */
export const PageLoader = ({ label = 'Loading', fullScreen = false }) => (
    <div className={`page-loader ${fullScreen ? 'is-fullscreen' : ''}`}>
        <FlowerLoader size={fullScreen ? 88 : 72} label={label} showLabel />
    </div>
);

export default HibiscusMark;

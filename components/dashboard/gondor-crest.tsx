/** Original, code-native White Tree mark; scales without raster blur. */
export function GondorCrest({ className = "" }: { className?: string }) {
  return <svg className={`gondor-crest ${className}`} viewBox="0 0 100 112" fill="none" aria-hidden="true" focusable="false">
    <path d="M50 6 87 21v41c0 23-22 39-37 46C35 101 13 85 13 62V21L50 6Z" fill="currentColor" fillOpacity=".04" stroke="currentColor" strokeWidth="1.4" />
    <path d="M50 14 80 26v35c0 19-17 33-30 39-13-6-30-20-30-39V26L50 14Z" stroke="currentColor" strokeOpacity=".4" />
    <g stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round">
      <path d="M50 38v42m0-21L37 48m13 8 13-12M50 70 33 60m17 5 18-10M50 80l-8 8m8-8 8 8M37 48v-9m0 9-10-2m36-2v-9m0 9 10-3M33 60l-7-1m7 1-1-7m36 2 7-4M45 88h-8m18 0h8" />
      <path d="m50 31 1.5 3 3.5.5-2.5 2.5.5 3.5-3-1.5-3 1.5.5-3.5-2.5-2.5 3.5-.5L50 31Z" strokeWidth="1" />
    </g>
    <g fill="currentColor">{[[-24,0],[-17,-7],[-8,-11],[0,-13],[8,-11],[17,-7],[24,0]].map(([x,y],i)=><path key={i} transform={`translate(${50+x} ${29+y})`} d="m0-2.6.8 1.8 1.8.8-1.8.8L0 2.6-.8.8-2.6 0l1.8-.8Z" />)}</g>
  </svg>;
}

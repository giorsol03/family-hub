import { Center, Image } from '@mantine/core'
import { IconToolsKitchen2 } from '@tabler/icons-react'
import { useState } from 'react'

/** Recipe photo, or a neutral placeholder when there's no URL or it fails to load. */
export function RecipeImage({ src, alt, height }: { src: string | null; alt: string; height: number }) {
  const [failed, setFailed] = useState(false)
  if (!src || failed) {
    return (
      <Center h={height} bg="var(--mantine-color-default-hover)" aria-hidden>
        <IconToolsKitchen2 size={Math.min(48, height / 2)} stroke={1.3} color="var(--mantine-color-dimmed)" />
      </Center>
    )
  }
  return <Image src={src} alt={alt} h={height} fit="cover" onError={() => setFailed(true)} />
}

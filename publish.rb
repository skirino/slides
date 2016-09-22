#!/usr/bin/env ruby

require 'yaml'
require 'fileutils'

slide_names  = Dir['slides/*'].map { |p| File.basename(p) }.sort
slide_titles = slide_names.map do |name|
  YAML.load(File.read("slides/#{name}/config.yml"))['title']
end

header = <<EOS
# Collection of my slides

[Table of contents](https://skirino.github.io/slides/)

>>>

EOS
links = slide_names.zip(slide_titles).map do |(name, title)|
  "- [#{title}](https://skirino.github.io/slides/#{name}.html)"
end
File.write('README.md', header + links.join("\n"))
FileUtils.cp('README.md', 'docs/README.md')

slide_names.each do |name|
  `cp slides/#{name}/slides/index.html docs/#{name}.html`
  images_dir = "slides/#{name}/images"
  `cp #{images_dir}/* docs/images/` if File.directory?(images_dir)
end
